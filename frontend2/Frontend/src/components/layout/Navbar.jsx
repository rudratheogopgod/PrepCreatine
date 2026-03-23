import { Link, useLocation } from "react-router-dom";
import {
  Brain,
  BarChart3,
  BookOpen,
  LayoutDashboard,
  Home,
} from "lucide-react";

export default function Navbar() {
  const location = useLocation();

  const navItems = [
    { name: "Home", icon: Home, path: "/" },
    { name: "Dashboard", icon: LayoutDashboard, path: "/dashboard" },
    { name: "Learn", icon: BookOpen, path: "/learn" },
    { name: "Tests", icon: Brain, path: "/test" },
    { name: "Progress", icon: BarChart3, path: "/progress" },
  ];

  return (
    <div className="w-full sticky top-0 z-50 backdrop-blur-lg bg-white/70 border-b border-gray-200 shadow-sm">

      <div className="max-w-7xl mx-auto px-6 py-3 flex items-center justify-between">

        {/* 🔥 LOGO */}
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-md">
            <Brain size={18} />
          </div>
          <h1 className="font-bold text-lg text-gray-800">
            AI Companion
          </h1>
        </div>

        {/* 🔥 NAV LINKS */}
        <div className="hidden md:flex items-center gap-4">

          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;

            return (
              <Link
                key={item.name}
                to={item.path}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                  isActive
                    ? "bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-md"
                    : "text-gray-600 hover:bg-gray-100"
                }`}
              >
                <Icon size={16} />
                {item.name}
              </Link>
            );
          })}
        </div>

        {/* 🔥 RIGHT SIDE */}
        <div className="flex items-center gap-3">

          {/* AI STATUS */}
          <div className="hidden sm:flex items-center gap-2 px-3 py-1 rounded-full bg-green-100 text-green-600 text-xs font-semibold">
            <span className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
            AI Active
          </div>

          {/* PROFILE */}
          <div className="w-9 h-9 rounded-full bg-gradient-to-r from-blue-500 to-purple-500 flex items-center justify-center text-white font-bold cursor-pointer">
            V
          </div>

        </div>

      </div>
    </div>
  );
}