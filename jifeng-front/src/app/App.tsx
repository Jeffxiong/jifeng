import { useState, useEffect } from "react";
import { PointsDetail } from "./components/PointsDetail";
import { PointsExchange } from "./components/PointsExchange";
import { Login } from "./components/Login";

type Page = "exchange" | "detail";

export default function App() {
  const [currentPage, setCurrentPage] = useState<Page>("exchange");
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  // 检查是否已登录
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      setIsLoggedIn(true);
    }
  }, []);

  const handleLoginSuccess = () => {
    setIsLoggedIn(true);
  };

  if (!isLoggedIn) {
    return <Login onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <div className="size-full">
      {currentPage === "exchange" && (
        <PointsExchange
          onNavigateToDetail={() => setCurrentPage("detail")}
        />
      )}
      {currentPage === "detail" && (
        <PointsDetail
          onBack={() => setCurrentPage("exchange")}
        />
      )}
    </div>
  );
}