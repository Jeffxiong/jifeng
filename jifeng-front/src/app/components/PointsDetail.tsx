import { useState, useEffect } from "react";
import { Card } from "./ui/card";
import { Button } from "./ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "./ui/dialog";
import { ChevronDown, Plus, Minus, ArrowLeft, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { pointsApi } from "../../services/api";

// 积分交互类型
type PointsType = "all" | "earned" | "spent";

// 时间筛选类型
type TimeRange = "30days" | "3months" | "12months" | "2years";

// 积分记录接口
interface PointsRecord {
  id: string | number;
  date: string;
  type: "earn" | "spend";
  points: number;
  description: string;
  balance: number;
  details?: string;
}


// 组件Props
interface PointsDetailProps {
  onBack?: () => void;
}

export function PointsDetail({ onBack }: PointsDetailProps) {
  const [pointsType, setPointsType] = useState<PointsType>("all");
  const [timeRange, setTimeRange] = useState<TimeRange>("30days");
  const [selectedRecord, setSelectedRecord] = useState<PointsRecord | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [records, setRecords] = useState<PointsRecord[]>([]);
  const [currentBalance, setCurrentBalance] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  // 加载积分明细和余额
  useEffect(() => {
    const loadData = async () => {
      try {
        setIsLoading(true);
        const [recordsData, balance] = await Promise.all([
          pointsApi.getRecords(pointsType, timeRange),
          pointsApi.getBalance(),
        ]);
        
        // 转换数据格式
        const formattedRecords = recordsData.map((r: any) => {
          let formattedDate = r.date;
          if (r.date) {
            try {
              const date = new Date(r.date);
              const year = date.getFullYear();
              const month = String(date.getMonth() + 1).padStart(2, '0');
              const day = String(date.getDate()).padStart(2, '0');
              const hours = String(date.getHours()).padStart(2, '0');
              const minutes = String(date.getMinutes()).padStart(2, '0');
              const seconds = String(date.getSeconds()).padStart(2, '0');
              formattedDate = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
            } catch (e) {
              console.error('日期格式化错误:', e);
            }
          }
          return {
            ...r,
            id: String(r.id),
            date: formattedDate,
          };
        });
        
        setRecords(formattedRecords);
        setCurrentBalance(balance);
      } catch (error) {
        console.error("加载数据失败:", error);
        toast.error("加载数据失败", {
          description: error instanceof Error ? error.message : "未知错误",
        });
      } finally {
        setIsLoading(false);
      }
    };
    loadData();
  }, [pointsType, timeRange]);

  // 筛选后的数据（后端已经筛选，这里直接使用）
  const filteredData = records;

  // 处理点击记录
  const handleRecordClick = (record: PointsRecord) => {
    if (record.details) {
      setSelectedRecord(record);
      setIsDialogOpen(true);
    }
  };

  // 返回处理
  const handleBack = () => {
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
        <div className="max-w-2xl mx-auto px-4 py-4 flex items-center gap-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleBack}
            className="hover:bg-gray-200"
          >
            <ArrowLeft className="w-4 h-4 mr-1" />
            返回
          </Button>
          <h1>积分明细</h1>
        </div>
      </div>

      <div className="max-w-2xl mx-auto p-4">
        {/* 头部 */}
        <div className="mb-6">
          {/* 当前积分余额卡片 */}
          <Card className="p-6 mb-4 bg-gradient-to-r from-blue-500 to-blue-600 text-white">
            <div className="text-center">
              <div className="text-sm opacity-90 mb-2">当前积分</div>
              <div className="text-4xl">{isLoading ? "..." : currentBalance}</div>
            </div>
          </Card>

          {/* 筛选条件 */}
          <div className="grid grid-cols-2 gap-3 mb-4">
            {/* 交互逻辑筛选 */}
            <Select value={pointsType} onValueChange={(value) => setPointsType(value as PointsType)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部</SelectItem>
                <SelectItem value="earned">获得</SelectItem>
                <SelectItem value="spent">消耗</SelectItem>
              </SelectContent>
            </Select>

            {/* 时间筛选 */}
            <Select value={timeRange} onValueChange={(value) => setTimeRange(value as TimeRange)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="30days">近30天</SelectItem>
                <SelectItem value="3months">近3个月</SelectItem>
                <SelectItem value="12months">近12个月</SelectItem>
                <SelectItem value="2years">近2年</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* 显示条 */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 flex items-center justify-between">
            <span className="text-sm text-blue-800">
              {pointsType === "all" && "显示全部交互"}
              {pointsType === "earned" && "仅显示获得积分"}
              {pointsType === "spent" && "仅显示消耗积分"}
            </span>
            <span className="text-sm text-blue-600">
              共 {filteredData.length} 条记录
            </span>
          </div>
        </div>

        {/* 积分列表 */}
        {isLoading ? (
          <div className="text-center py-12">
            <Loader2 className="w-8 h-8 animate-spin mx-auto mb-4" />
            <p>加载中...</p>
          </div>
        ) : (
          <div className="space-y-3">
            {filteredData.map((record) => (
            <Card
              key={record.id}
              className="p-4 cursor-pointer hover:shadow-md transition-shadow"
              onClick={() => handleRecordClick(record)}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    {record.type === "earn" ? (
                      <div className="w-8 h-8 rounded-full bg-green-100 flex items-center justify-center">
                        <Plus className="w-4 h-4 text-green-600" />
                      </div>
                    ) : (
                      <div className="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center">
                        <Minus className="w-4 h-4 text-red-600" />
                      </div>
                    )}
                    <div>
                      <div className="text-gray-900">{record.description}</div>
                      <div className="text-sm text-gray-500">{record.date}</div>
                    </div>
                  </div>
                </div>
                <div className="text-right">
                  <div
                    className={`${
                      record.type === "earn" ? "text-green-600" : "text-red-600"
                    }`}
                  >
                    {record.type === "earn" ? "+" : ""}
                    {record.points}
                  </div>
                  <div className="text-sm text-gray-500">余额: {record.balance}</div>
                </div>
              </div>
              {record.details && (
                <div className="flex items-center justify-end mt-2 text-sm text-blue-600">
                  <span>查看详情</span>
                  <ChevronDown className="w-4 h-4 ml-1" />
                </div>
              )}
            </Card>
            ))}

            {filteredData.length === 0 && (
              <div className="text-center py-12 text-gray-400">
                <div className="text-lg mb-2">暂无记录</div>
                <div className="text-sm">当前筛选条件下没有积分记录</div>
              </div>
            )}
          </div>
        )}

        {/* 详情弹窗 */}
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>积分详情</DialogTitle>
              <DialogDescription className="sr-only">
                查看积分交互的详细信息
              </DialogDescription>
            </DialogHeader>
            {selectedRecord && (
              <div className="space-y-4">
                <div className="flex items-center justify-between py-3 border-b">
                  <span className="text-gray-600">交互类型</span>
                  <span className="text-gray-900">
                    {selectedRecord.type === "earn" ? "获得积分" : "消耗积分"}
                  </span>
                </div>
                <div className="flex items-center justify-between py-3 border-b">
                  <span className="text-gray-600">积分数量</span>
                  <span
                    className={`${
                      selectedRecord.type === "earn" ? "text-green-600" : "text-red-600"
                    }`}
                  >
                    {selectedRecord.type === "earn" ? "+" : ""}
                    {selectedRecord.points}
                  </span>
                </div>
                <div className="flex items-center justify-between py-3 border-b">
                  <span className="text-gray-600">交互时间</span>
                  <span className="text-gray-900">{selectedRecord.date}</span>
                </div>
                <div className="flex items-center justify-between py-3 border-b">
                  <span className="text-gray-600">积分余额</span>
                  <span className="text-gray-900">{selectedRecord.balance}</span>
                </div>
                {selectedRecord.details && (
                  <div className="py-3">
                    <div className="text-gray-600 mb-2">详细说明</div>
                    <div className="text-gray-900 bg-gray-50 p-3 rounded-lg">
                      {selectedRecord.details}
                    </div>
                  </div>
                )}
                <Button
                  onClick={() => setIsDialogOpen(false)}
                  className="w-full"
                >
                  确定
                </Button>
              </div>
            )}
          </DialogContent>
        </Dialog>
      </div>
    </div>
  );
}