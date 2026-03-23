import { Brain, Sparkles, BarChart3, ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import CircularText from "../../components/other/CircularText";
import LiquidEther from "../../components/other/HomeAnimation/LiquidEther";

export default function Home() {
  return (
    <div>
      <div>
        <div
          style={{
            width: "100%",
            height: 600,
            position: "absolute",
            zIndex: "50",
          }}
        >
          <LiquidEther
            colors={["#6D28D9", "#4F46E5", "#0EA5E9"]}
            mouseForce={15}
            cursorSize={90}
            isViscous
            viscous={25}
            iterationsViscous={28}
            iterationsPoisson={28}
            resolution={0.6}
            isBounce={false}
            autoDemo
            autoSpeed={0.4}
            autoIntensity={1.8}
            takeoverDuration={0.3}
            autoResumeDelay={2500}
            autoRampDuration={0.8}
            color0="#6D28D9" // purple
            color1="#4F46E5" // indigo
            color2="#0EA5E9" // blue
          />
        </div>
      </div>
      <div className="min-h-screen relative overflow-hidden bg-gradient-to-br from-slate-50 via-white to-blue-50">
        {/* 🔥 HERO */}
        <div className="relative   max-w-7xl mx-auto px-6 py-28 text-center">
          <motion.div
            initial={{ opacity: 0, scale: 0.7 }}
            animate={{ opacity: 1, scale: 1 }}
            className="flex justify-center mb-6"
          >
            <div className="p-5 rounded-3xl bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-2xl">
              <Brain size={42} />
            </div>
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-5xl md:text-6xl font-bold text-gray-900 leading-tight"
          >
            Learn Smarter with
            <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-purple-600 to-blue-600">
              AI Intelligence
            </span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="mt-6 text-gray-600 text-lg max-w-2xl mx-auto"
          >
            Your personal AI that remembers, analyzes and improves your learning
            journey.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-10 flex justify-center gap-4"
          >
            <Link to="/dashboard">
              <button className="px-7 py-3 bg-gradient-to-r from-purple-600 to-indigo-600 text-white rounded-2xl shadow-xl hover:scale-105 transition">
                Get Started <ArrowRight className="inline ml-2" size={18} />
              </button>
            </Link>

            <Link to="/test">
              <button className="px-7 py-3 bg-white/70 backdrop-blur border rounded-2xl hover:bg-white transition">
                Try Demo
              </button>
            </Link>
          </motion.div>
        </div>

        {/* 🔥 FEATURES */}
        <div className="relative max-w-6xl mx-auto px-6 py-20 grid md:grid-cols-3 gap-8">
          {[
            {
              icon: Brain,
              title: "AI Adaptive System",
              desc: "Automatically adapts difficulty based on your performance.",
            },
            {
              icon: BarChart3,
              title: "Deep Analytics",
              desc: "Understand your strengths and weaknesses clearly.",
            },
            {
              icon: Sparkles,
              title: "Smart Revision",
              desc: "AI ensures you revise at the perfect time.",
            },
          ].map((item, i) => {
            const Icon = item.icon;

            return (
              <motion.div
                key={i}
                initial={{ opacity: 0, y: 50 }}
                whileInView={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.2 }}
                className="p-6 rounded-3xl bg-white/70 backdrop-blur-lg shadow-lg border hover:shadow-2xl hover:-translate-y-2 transition"
              >
                <Icon className="text-purple-600 mb-4" size={32} />
                <h3 className="font-semibold text-lg mb-2">{item.title}</h3>
                <p className="text-gray-600 text-sm">{item.desc}</p>
              </motion.div>
            );
          })}
        </div>
        {/* 🔥 EVERYTHING YOU NEED SECTION */}
        <div className="py-24 px-6 bg-gray-50">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-gray-900">
              Everything you need,
              <br />
              nothing you don't
            </h2>
          </div>

          <div className="max-w-7xl mx-auto grid md:grid-cols-5 gap-6">
            {[
              {
                title: "AI Tutor",
                desc: "Ask anything. Get step-by-step answers anytime.",
                color: "text-blue-500",
                icon: "🧠",
              },
              {
                title: "Smart Roadmap",
                desc: "Dynamic study schedule adapts to your progress.",
                color: "text-purple-500",
                icon: "🗺️",
              },
              {
                title: "Mock Tests",
                desc: "Real exam interface. Instant detailed analytics.",
                color: "text-orange-500",
                icon: "📋",
              },
              {
                title: "Analytics",
                desc: "Know exactly where you lose marks and why.",
                color: "text-green-500",
                icon: "📊",
              },
              {
                title: "Smart Notes",
                desc: "Upload PDFs. AI generates flashcards automatically.",
                color: "text-cyan-500",
                icon: "📄",
              },
            ].map((item, i) => (
              <div
                key={i}
                className="p-6 rounded-3xl bg-white shadow-md hover:shadow-xl hover:-translate-y-2 transition"
              >
                <div className="text-2xl mb-4">{item.icon}</div>

                <h3 className="font-semibold text-lg mb-2">{item.title}</h3>

                <p className="text-gray-600 text-sm">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>

        {/* 🔥 HOW IT WORKS */}
        <div className="py-20 px-6 bg-white/50 backdrop-blur">
          <h2 className="text-center text-3xl font-bold mb-12">
            How It Works ⚡
          </h2>

          <div className="max-w-5xl mx-auto grid md:grid-cols-3 gap-8 text-center">
            {["Take Test", "AI Analyze", "Improve Smartly"].map((step, i) => (
              <motion.div
                key={i}
                whileHover={{ scale: 1.05 }}
                className="p-8 rounded-2xl bg-white shadow-md border"
              >
                <div className="text-3xl font-bold text-purple-600 mb-3">
                  {i + 1}
                </div>
                <p className="font-medium">{step}</p>
              </motion.div>
            ))}
          </div>
        </div>

        {/* 🔥 CTA */}
        <div className="py-20 bg-gradient-to-r flex from-purple-600 to-indigo-600 text-white">
          <div className="max-w-6xl mx-auto px-6 text-left">
            <motion.h2
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              className="text-4xl font-bold mb-4"
            >
              Ready to Level Up? 🚀
            </motion.h2>

            <p className="mb-6 opacity-90 text-lg max-w-xl">
              Start your AI-powered learning journey today.
            </p>

            <Link to="/dashboard">
              <button className="px-7 py-3 bg-white text-purple-600 rounded-2xl font-semibold hover:scale-105 transition shadow-lg">
                Go to Dashboard
              </button>
            </Link>
          </div>
          <div className="   mx-70  ">
            <CircularText
              text=" AI • SMART • LEARN • GROW • "
              onHover="speedUp"
              spinDuration={20}
              className="custom-class"
            />
          </div>
        </div>
      </div>
    </div>
  );
}
