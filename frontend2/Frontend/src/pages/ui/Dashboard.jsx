import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Brain, Calendar, Target, BookOpen, ClipboardList } from "lucide-react";
import api from "../../api/axios";

export default function Dashboard() {
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const examDate = new Date("2026-05-15");
  const today = new Date();
  const daysLeft = Math.ceil((examDate - today) / (1000 * 60 * 60 * 24));

  useEffect(() => {
    api.get("/analytics")
      .then((res) => setAnalytics(res.data))
      .catch(() => setError("Failed to load analytics."))
      .finally(() => setLoading(false));
  }, []);

  const stats = analytics?.stats || {};
  const completedTopics = stats.topicsCompleted ?? 0;
  const totalTopics = 120;
  const testsGiven = stats.testsTaken ?? 0;
  const totalTests = 20;
  const avgScore = stats.avgScore ?? 0;

  const Card = ({ icon: Icon, title, value, extra, gradient }) => (
    <motion.div
      whileHover={{ y: -5, scale: 1.02 }}
      className={`p-6 rounded-3xl text-white shadow-lg ${gradient}`}
    >
      <div className="flex items-center gap-3 mb-4">
        <div className="p-2 bg-white/20 rounded-xl">
          <Icon size={18} />
        </div>
        <p className="text-sm opacity-80">{title}</p>
      </div>
      <h2 className="text-2xl font-bold">{value}</h2>
      {extra && <p className="text-sm mt-2 opacity-80">{extra}</p>}
    </motion.div>
  );

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-500">
        Loading dashboard...
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center text-red-500">
        {error}
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-blue-50 p-6">
      <div className="mb-10">
        <h1 className="text-3xl font-bold text-gray-900">AI Learning Dashboard 🧠</h1>
        <p className="text-gray-500">Your personalized AI learning system</p>
      </div>

      <div className="grid md:grid-cols-4 gap-6 mb-8">
        <Card icon={Calendar} title="Exam Date" value={examDate.toDateString()} gradient="bg-gradient-to-r from-purple-600 to-indigo-600" />
        <Card icon={Target} title="Days Left" value={`${daysLeft} days`} gradient="bg-gradient-to-r from-blue-500 to-indigo-500" />
        <Card icon={BookOpen} title="Topics Progress" value={`${completedTopics}/${totalTopics}`} extra="Keep learning daily 📚" gradient="bg-gradient-to-r from-cyan-500 to-blue-500" />
        <Card icon={ClipboardList} title="Tests Given" value={`${testsGiven}/${totalTests}`} extra={`Avg score: ${avgScore}%`} gradient="bg-gradient-to-r from-green-500 to-emerald-600" />
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        <motion.div whileHover={{ scale: 1.02 }} className="p-6 rounded-3xl bg-white shadow-md">
          <h2 className="font-semibold mb-4 text-gray-800">📊 Learning Progress</h2>
          <p className="text-sm text-gray-500 mb-2">Topics Completed</p>
          <div className="w-full h-3 bg-gray-200 rounded-full overflow-hidden">
            <div
              className="h-3 bg-gradient-to-r from-blue-500 to-indigo-600 rounded-full transition-all duration-700"
              style={{ width: `${(completedTopics / totalTopics) * 100}%` }}
            />
          </div>
          <p className="text-xs text-gray-500 mt-2">
            {Math.round((completedTopics / totalTopics) * 100)}% completed
          </p>
        </motion.div>

        <motion.div whileHover={{ scale: 1.02 }} className="p-6 rounded-3xl bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-lg">
          <div className="flex items-center gap-2 mb-3">
            <Brain />
            <h2 className="font-semibold">AI Insight</h2>
          </div>
          <p className="text-sm opacity-90">
            {analytics?.aiInsight || "Keep studying consistently! Your AI tutor is analysing your progress."}
          </p>
          <div className="mt-4 bg-white/20 px-3 py-2 rounded-xl text-sm">
            Streak: {stats.currentStreak ?? 0} day{stats.currentStreak !== 1 ? "s" : ""} 🔥
          </div>
        </motion.div>
      </div>

      <div className="mt-10">
        <h2 className="font-semibold mb-4 text-gray-800">🚀 Quick Actions</h2>
        <div className="grid md:grid-cols-3 gap-6">
          {[
            { label: "Start New Test", path: "/test" },
            { label: "Revise Weak Topics", path: "/planner" },
            { label: "View Full Analytics", path: "/knowledge" },
          ].map((item, i) => (
            <a key={i} href={item.path}>
              <motion.div
                whileHover={{ scale: 1.05 }}
                className="p-6 bg-white rounded-2xl shadow-md text-center cursor-pointer hover:shadow-xl transition"
              >
                {item.label}
              </motion.div>
            </a>
          ))}
        </div>
      </div>
    </div>
  );
}
