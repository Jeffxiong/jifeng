import { useState, useEffect } from "react";
import { Card } from "./ui/card";
import { Button } from "./ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "./ui/dialog";
import { Input } from "./ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { ArrowLeft, ChevronDown, ChevronUp, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { productApi, pointsApi } from "../../services/api";

// 产品接口
interface Product {
  id: string | number;
  name: string;
  points: number;
  description: string;
  stock: number;
  image: string;
  monthlyLimit: number;
  usedThisMonth: number;
}

// 组件Props
interface PointsExchangeProps {
  onNavigateToDetail?: () => void;
  onBack?: () => void;
}


// 兑换规则
const exchangeRules = [
  "每个商品每月有兑换次数限制，超过限制后当月无法继续兑换",
  "兑换成功后，优惠券将在24小时内发放至您的账户，请注意查收",
  "所有优惠券有效期为30天，过期自动失效，请及时使用",
];

export function PointsExchange({ onNavigateToDetail, onBack }: PointsExchangeProps) {
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [isExchangeDialogOpen, setIsExchangeDialogOpen] = useState(false);
  const [isConfirmDialogOpen, setIsConfirmDialogOpen] = useState(false);
  const [quantity, setQuantity] = useState(1);
  const [verificationCode, setVerificationCode] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [codeSent, setCodeSent] = useState(false);
  const [countdown, setCountdown] = useState(0); // 倒计时秒数
  const [verificationError, setVerificationError] = useState(""); // 验证码错误提示
  const [isRulesExpanded, setIsRulesExpanded] = useState(false);
  const [showMoreProducts, setShowMoreProducts] = useState(false);
  const [products, setProducts] = useState<Product[]>([]);
  const [currentPoints, setCurrentPoints] = useState(0);
  const [isLoadingData, setIsLoadingData] = useState(true);

  // 加载产品列表和积分余额
  useEffect(() => {
    const loadData = async () => {
      try {
        setIsLoadingData(true);
        
        // 分别加载产品列表和积分余额，避免一个失败影响另一个
        const loadProducts = async () => {
          try {
            const productsData = await productApi.getProducts();
            // 转换数据格式，确保id为字符串
            const formattedProducts = productsData.map((p: any) => ({
              ...p,
              id: String(p.id),
            }));
            setProducts(formattedProducts);
          } catch (error) {
            console.error("加载产品列表失败:", error);
            toast.error("加载产品列表失败", {
              description: error instanceof Error ? error.message : "未知错误",
            });
          }
        };
        
        const loadBalance = async () => {
          try {
            const balance = await pointsApi.getBalance();
            setCurrentPoints(balance);
          } catch (error) {
            console.error("加载积分余额失败:", error);
            // 积分余额加载失败不影响页面显示，只显示提示
            toast.warning("加载积分余额失败", {
              description: error instanceof Error ? error.message : "未知错误",
            });
            setCurrentPoints(0); // 设置为0，避免显示undefined
          }
        };
        
        // 并行加载，但各自处理错误
        await Promise.allSettled([
          loadProducts(),
          loadBalance(),
        ]);
      } catch (error) {
        console.error("加载数据失败:", error);
      } finally {
        setIsLoadingData(false);
      }
    };
    loadData();
  }, []);

  // 显示的产品数量
  const displayedProducts = showMoreProducts ? products : products.slice(0, 8);

  // 处理兑换点击
  const handleExchangeClick = (product: Product) => {
    setSelectedProduct(product);
    setQuantity(1);
    setIsExchangeDialogOpen(true);
  };

  // 计算剩余兑换次数
  const getRemainingExchanges = (product: Product) => {
    return product.monthlyLimit - product.usedThisMonth;
  };

  // 处理确认兑换
  const handleConfirmExchange = () => {
    if (!selectedProduct) return;

    const remaining = getRemainingExchanges(selectedProduct);
    if (remaining === 0) {
      toast.error("本月已兑完", {
        description: "该商品本月剩余兑换次数为0，请关注积分返还页面或等待下月刷新。",
      });
      return;
    }

    if (quantity > remaining) {
      toast.error("超过兑换限制", {
        description: `本月最多还可兑换 ${remaining} 次`,
      });
      return;
    }

    if (currentPoints < selectedProduct.points * quantity) {
      toast.error("积分不足", {
        description: "您的积分余额不足以完成此次兑换",
      });
      return;
    }

    // 检查库存
    if (selectedProduct.stock < quantity) {
      toast.error("库存不足", {
        description: `当前库存：${selectedProduct.stock} 件，需要：${quantity} 件`,
      });
      return;
    }

    setIsExchangeDialogOpen(false);
    setIsConfirmDialogOpen(true);
  };

  // 发送短信验证码
  const handleSendCode = async () => {
    if (countdown > 0) return; // 倒计时期间不允许发送
    
    setIsSendingCode(true);
    setVerificationError(""); // 清除之前的错误提示
    try {
      const code = await pointsApi.sendSmsCode();
      setCodeSent(true);
      setCountdown(60); // 开始60秒倒计时
      // 开发环境显示验证码，生产环境不应显示
      toast.success("验证码已发送", {
        description: `验证码：${code}（开发环境显示，生产环境不显示）`,
        duration: 5000,
      });
    } catch (error) {
      toast.error("发送验证码失败", {
        description: error instanceof Error ? error.message : "未知错误",
      });
    } finally {
      setIsSendingCode(false);
    }
  };

  // 倒计时效果
  useEffect(() => {
    let timer: NodeJS.Timeout | null = null;
    if (countdown > 0) {
      timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => {
      if (timer) {
        clearInterval(timer);
      }
    };
  }, [countdown]);

  // 处理最终确认兑换
  const handleFinalConfirm = async () => {
    if (!selectedProduct) return;

    if (!verificationCode || verificationCode.trim() === "") {
      setVerificationError("请输入验证码");
      toast.error("请输入验证码", {
        description: "请先发送并输入验证码",
      });
      return;
    }

    setIsLoading(true);
    setVerificationError(""); // 清除之前的错误提示
    try {
      // productId 现在是 UUID 字符串
      const productId = String(selectedProduct.id);
      
      await pointsApi.exchange(productId, quantity, verificationCode);
      
      setIsConfirmDialogOpen(false);
      setVerificationCode("");
      setCodeSent(false);
      setCountdown(0); // 重置倒计时
      setVerificationError(""); // 清除错误提示
      
      // 刷新数据
      const [productsData, balance] = await Promise.all([
        productApi.getProducts(),
        pointsApi.getBalance(),
      ]);
      
      const formattedProducts = productsData.map((p: any) => ({
        ...p,
        id: String(p.id),
      }));
      
      setProducts(formattedProducts);
      setCurrentPoints(balance);
      
      // 显示成功提示
      toast.success("🎉 兑换成功！", {
        description: `您已成功兑换 ${selectedProduct.name} x${quantity}，优惠券将在24小时内发放至您的账户`,
        duration: 5000,
      });
    } catch (error) {
      // 显示错误提示
      const errorMessage = error instanceof Error ? error.message : "未知错误";
      console.error("兑换失败:", error);
      
      // 如果是验证码相关的错误，显示在输入框下方
      if (errorMessage.includes("验证码") || errorMessage.includes("验证码错误") || errorMessage.includes("验证码已过期")) {
        setVerificationError(errorMessage);
        toast.error("验证码错误", {
          description: errorMessage,
        });
      } else {
        setVerificationError(""); // 其他错误不清除输入框错误提示
        let description = errorMessage;
        if (errorMessage.includes("积分不足")) {
          description = "您的积分不足，请先获取更多积分";
        } else if (errorMessage.includes("库存不足")) {
          description = "商品库存不足，请选择其他商品";
        } else if (errorMessage.includes("月度兑换限制") || errorMessage.includes("超过月度")) {
          // 提取详细信息
          const match = errorMessage.match(/本月已兑换\s*(\d+)\s*次.*限制\s*(\d+)\s*次.*剩余\s*(\d+)\s*次/);
          if (match) {
            description = `您已达到本月兑换上限（已兑换 ${match[1]} 次，限制 ${match[2]} 次），请下月再试或选择其他商品`;
          } else {
            description = "您已达到本月兑换上限，请下月再试或选择其他商品";
          }
        } else if (errorMessage.includes("手机号")) {
          description = "请先绑定手机号";
        } else if (errorMessage.includes("产品不存在")) {
          description = "商品不存在或已下架，请刷新页面";
        } else if (errorMessage.includes("用户不存在")) {
          description = "用户信息异常，请重新登录";
        }
        
        toast.error("兑换失败", {
          description: description,
          duration: 5000,
        });
      }
    } finally {
      setIsLoading(false);
    }
  };

  // 返回积分中心
  const handleBack = () => {
    // 这里应该导航回积分中心首页
    if (onBack) {
      onBack();
    } else {
      window.history.back();
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 顶部导航 */}
      <div className="bg-white border-b sticky top-0 z-10">
        <div className="max-w-6xl mx-auto px-4 py-4 flex items-center gap-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleBack}
            className="hover:bg-gray-200"
          >
            <ArrowLeft className="w-4 h-4 mr-1" />
            返回
          </Button>
          <h1>积分兑换</h1>
        </div>
      </div>

      <div className="max-w-6xl mx-auto p-4">
        {/* 当前积分显示 */}
        <Card className="p-4 mb-6 bg-gradient-to-r from-blue-500 to-blue-600 text-white">
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm opacity-90">当前可用积分</div>
              <div className="text-3xl mt-1">{currentPoints}</div>
            </div>
            <div className="flex items-center gap-3">
              <div className="text-sm opacity-90">
                兑换后立即扣除相应积分
              </div>
              <Button
                variant="outline"
                size="sm"
                className="bg-white text-blue-600 hover:bg-blue-50 border-none"
                onClick={onNavigateToDetail}
              >
                详情
              </Button>
            </div>
          </div>
        </Card>

        {/* 兑换规则 */}
        <Card className="p-4 mb-6">
          <div
            className="flex items-center justify-between cursor-pointer"
            onClick={() => setIsRulesExpanded(!isRulesExpanded)}
          >
            <div className="flex items-center gap-2">
              <span>兑换须知</span>
            </div>
            {isRulesExpanded ? (
              <ChevronUp className="w-5 h-5 text-gray-500" />
            ) : (
              <ChevronDown className="w-5 h-5 text-gray-500" />
            )}
          </div>
          {isRulesExpanded && (
            <div className="mt-4 space-y-2">
              {exchangeRules.map((rule, index) => (
                <div
                  key={index}
                  className="text-sm text-gray-700 bg-orange-50 border-l-4 border-orange-400 p-3 rounded"
                >
                  {index + 1}. {rule}
                </div>
              ))}
            </div>
          )}
        </Card>

        {/* 产品列表 */}
        {isLoadingData ? (
          <div className="text-center py-12">
            <Loader2 className="w-8 h-8 animate-spin mx-auto mb-4" />
            <p>加载中...</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            {displayedProducts.map((product) => {
            const remaining = getRemainingExchanges(product);
            const isOutOfStock = remaining === 0 || product.stock === 0;
            
            return (
              <Card
                key={product.id}
                className={`overflow-hidden cursor-pointer transition-all hover:shadow-lg ${
                  selectedProduct?.id === product.id ? "ring-2 ring-green-500" : ""
                }`}
                onClick={() => !isOutOfStock && setSelectedProduct(product)}
              >
                <div className="relative">
                  <img
                    src={product.image}
                    alt={product.name}
                    className="w-full h-40 object-cover"
                  />
                  {isOutOfStock && (
                    <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                      <span className="text-white text-lg">
                        {product.stock === 0 ? "库存不足" : "本月已兑完"}
                      </span>
                    </div>
                  )}
                </div>
                <div className="p-4">
                  <h3 className="mb-2">{product.name}</h3>
                  <p className="text-sm text-gray-600 mb-3">{product.description}</p>
                  <div className="flex items-center justify-between mb-2">
                    <div className="text-orange-600">
                      {product.points} 积分
                    </div>
                    <div className="text-xs text-gray-500">
                      剩余 {remaining}/{product.monthlyLimit} 次
                    </div>
                  </div>
                  <div className="text-xs text-gray-500 mb-2">
                    库存：{product.stock} 件
                  </div>
                  <Button
                    className="w-full mt-3"
                    disabled={isOutOfStock}
                    onClick={(e) => {
                      e.stopPropagation();
                      handleExchangeClick(product);
                    }}
                  >
                    {isOutOfStock ? (product.stock === 0 ? "库存不足" : "本月已兑完") : "立即兑换"}
                  </Button>
                </div>
              </Card>
            );
          })}
          </div>
        )}

        {/* 查看更多按钮 */}
        {!showMoreProducts && products.length > 8 && (
          <div className="text-center mb-6">
            <Button
              variant="outline"
              onClick={() => setShowMoreProducts(true)}
            >
              查看更多商品
            </Button>
          </div>
        )}

        {/* 兑换对话框 */}
        <Dialog open={isExchangeDialogOpen} onOpenChange={setIsExchangeDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>选择兑换数量</DialogTitle>
              <DialogDescription className="sr-only">
                选择您要兑换的商品数量
              </DialogDescription>
            </DialogHeader>
            {selectedProduct && (
              <div className="space-y-4">
                <div className="flex items-center gap-4">
                  <img
                    src={selectedProduct.image}
                    alt={selectedProduct.name}
                    className="w-20 h-20 object-cover rounded"
                  />
                  <div className="flex-1">
                    <h3>{selectedProduct.name}</h3>
                    <p className="text-sm text-gray-600">{selectedProduct.description}</p>
                    <p className="text-orange-600 mt-1">
                      {selectedProduct.points} 积分/个
                    </p>
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-sm text-gray-600">兑换数量</label>
                  <Select
                    value={quantity.toString()}
                    onValueChange={(value) => setQuantity(parseInt(value))}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {Array.from(
                        { length: getRemainingExchanges(selectedProduct) },
                        (_, i) => i + 1
                      ).map((num) => (
                        <SelectItem key={num} value={num.toString()}>
                          {num}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <p className="text-xs text-gray-500">
                    本月剩余兑换次数：{getRemainingExchanges(selectedProduct)} 次
                  </p>
                </div>

                <div className="bg-blue-50 p-4 rounded-lg">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-gray-700">所需积分</span>
                    <span className="text-lg text-orange-600">
                      {selectedProduct.points * quantity}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-gray-700">当前积分</span>
                    <span className="text-lg">{currentPoints}</span>
                  </div>
                  <div className="border-t border-blue-200 mt-2 pt-2 flex items-center justify-between">
                    <span className="text-gray-700">兑换后剩余</span>
                    <span
                      className={`text-lg ${
                        currentPoints - selectedProduct.points * quantity < 0
                          ? "text-red-600"
                          : "text-green-600"
                      }`}
                    >
                      {currentPoints - selectedProduct.points * quantity}
                    </span>
                  </div>
                </div>

                <div className="flex gap-3">
                  <Button
                    variant="outline"
                    className="flex-1"
                    onClick={() => setIsExchangeDialogOpen(false)}
                  >
                    取消
                  </Button>
                  <Button
                    className="flex-1"
                    onClick={handleConfirmExchange}
                  >
                    我想兑换
                  </Button>
                </div>
              </div>
            )}
          </DialogContent>
        </Dialog>

        {/* 确认兑��对话框 */}
        <Dialog open={isConfirmDialogOpen} onOpenChange={setIsConfirmDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>确认兑换</DialogTitle>
              <DialogDescription className="sr-only">
                输入验证码确认兑换
              </DialogDescription>
            </DialogHeader>
            {selectedProduct && (
              <div className="space-y-4">
                <div className="bg-gray-50 p-4 rounded-lg space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-600">商品名称</span>
                    <span>{selectedProduct.name}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-gray-600">兑换数量</span>
                    <span>{quantity}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-gray-600">消耗积分</span>
                    <span className="text-orange-600">
                      {selectedProduct.points * quantity}
                    </span>
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-sm text-gray-600">
                    短信验证码
                  </label>
                  <div className="flex gap-2">
                    <Input
                      type="text"
                      placeholder="请输入短信验证码"
                      value={verificationCode}
                      onChange={(e) => {
                        setVerificationCode(e.target.value);
                        setVerificationError(""); // 输入时清除错误提示
                      }}
                      maxLength={6}
                      className={`flex-1 ${verificationError ? "border-red-500" : ""}`}
                    />
                    <Button
                      type="button"
                      variant="outline"
                      onClick={handleSendCode}
                      disabled={isSendingCode || countdown > 0}
                    >
                      {isSendingCode ? (
                        <>
                          <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                          发送中
                        </>
                      ) : countdown > 0 ? (
                        `${countdown}秒后重发`
                      ) : (
                        "发送验证码"
                      )}
                    </Button>
                  </div>
                  {codeSent && !verificationError && (
                    <p className="text-xs text-gray-500">
                      验证码已发送到您的手机，请查收
                    </p>
                  )}
                  {verificationError && (
                    <p className="text-xs text-red-500 flex items-center gap-1">
                      <span>⚠️</span>
                      <span>{verificationError}</span>
                    </p>
                  )}
                </div>

                <div className="flex gap-3">
                  <Button
                    variant="outline"
                    className="flex-1"
                    onClick={() => {
                      setIsConfirmDialogOpen(false);
                      setVerificationCode("");
                      setCodeSent(false);
                      setCountdown(0); // 重置倒计时
                    }}
                    disabled={isLoading}
                  >
                    取消
                  </Button>
                  <Button
                    className="flex-1"
                    onClick={handleFinalConfirm}
                    disabled={isLoading || !verificationCode}
                  >
                    {isLoading ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        兑换中...
                      </>
                    ) : (
                      "确认兑换"
                    )}
                  </Button>
                </div>
              </div>
            )}
          </DialogContent>
        </Dialog>
      </div>
    </div>
  );
}