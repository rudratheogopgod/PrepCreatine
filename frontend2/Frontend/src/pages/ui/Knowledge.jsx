import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import api from "../../api/axios";

export default function Knowledge() {
  const [topics, setTopics] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get("/analytics/topics")
      .then((res) => setTopics(res.data || []))
      .catch(() => setTopics([]))
      .finally(() => setLoading(false));
  }, []);

  const getMasteryColor = (pct) => {
    if (pct >= 80) return "bg-green-500";
    if (pct >= 50) return "bg-yellow-400";
    return "bg-red-400";
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-400">
        Loading knowledge map...
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-blue-50 p-6">
      <div className="max-w-5xl mx-auto">
        <h1 className="text-3xl font-bold mb-2 text-gray-900">Knowledge Map 🗺️</h1>
        <p className="text-gray-500 mb-8">Your mastery level across all topics</p>

        {topics.length === 0 ? (
          <div className="p-8 bg-white rounded-2xl shadow text-center text-gray-500">
            No topic data yet. Complete some sessions to see your knowledge map!
          </div>
        ) : (
          <div className="grid md:grid-cols-2 gap-4">
            {topics.map((topic, i) => {
              const mastery = Number(topic.masteryPct ?? topic.mastery ?? 0);
              return (
                <motion.div
                  key={i}
                  whileHover={{ scale: 1.02 }}
                  className="bg-white rounded-2xl p-4 shadow border"
                >
                  <div className="flex justify-between mb-2">
                    <div>
                      <p className="font-semibold text-gray-800 text-sm">{topic.topicName ?? topic.name}</p>
                      <p className="text-xs text-gray-400">{topic.subjectName ?? topic.subject}</p>
                    </div>
                    <span className="text-sm font-bold text-gray-700">{mastery}%</span>
                  </div>
                  <div className="w-full h-2 bg-gray-100 rounded-full">
                    <div
                      className={`h-2 rounded-full transition-all ${getMasteryColor(mastery)}`}
                      style={{ width: `${mastery}%` }}
                    />
                  </div>
                </motion.div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}