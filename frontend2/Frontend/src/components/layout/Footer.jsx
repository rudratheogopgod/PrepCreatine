import { Brain, Github, Linkedin, Mail } from "lucide-react";

export default function Footer() {
  return (
    <footer className="bg-gradient-to-r from-slate-900 to-slate-800 text-white pt-16 pb-8 mt-20">

      <div className="max-w-7xl mx-auto px-6 grid md:grid-cols-4 gap-10">

        {/* 🔥 LOGO + DESC */}
        <div>
          <div className="flex items-center gap-2 mb-4">
            <div className="p-2 bg-gradient-to-r from-purple-600 to-indigo-600 rounded-xl">
              <Brain size={20} />
            </div>
            <h2 className="font-bold text-lg">AI Companion</h2>
          </div>

          <p className="text-sm text-gray-400">
            An intelligent AI system that helps students learn smarter,
            track performance, and never forget.
          </p>
        </div>

        {/* 🔥 NAV LINKS */}
        <div>
          <h3 className="font-semibold mb-4">Product</h3>
          <ul className="space-y-2 text-sm text-gray-400">
            <li className="hover:text-white cursor-pointer">Dashboard</li>
            <li className="hover:text-white cursor-pointer">Tests</li>
            <li className="hover:text-white cursor-pointer">Analytics</li>
            <li className="hover:text-white cursor-pointer">AI Tutor</li>
          </ul>
        </div>

        {/* 🔥 COMPANY */}
        <div>
          <h3 className="font-semibold mb-4">Company</h3>
          <ul className="space-y-2 text-sm text-gray-400">
            <li className="hover:text-white cursor-pointer">About</li>
            <li className="hover:text-white cursor-pointer">Contact</li>
            <li className="hover:text-white cursor-pointer">Privacy</li>
            <li className="hover:text-white cursor-pointer">Terms</li>
          </ul>
        </div>

        {/* 🔥 SOCIAL */}
        <div>
          <h3 className="font-semibold mb-4">Connect</h3>

          <div className="flex gap-4">

            <div className="p-2 bg-white/10 rounded-lg hover:bg-white/20 cursor-pointer transition">
              <Github size={18} />
            </div>

            <div className="p-2 bg-white/10 rounded-lg hover:bg-white/20 cursor-pointer transition">
              <Linkedin size={18} />
            </div>

            <div className="p-2 bg-white/10 rounded-lg hover:bg-white/20 cursor-pointer transition">
              <Mail size={18} />
            </div>

          </div>

          <p className="text-sm text-gray-400 mt-4">
            Built for Hackathon 🚀
          </p>
        </div>

      </div>

      {/* 🔥 BOTTOM */}
      <div className="border-t border-gray-700 mt-12 pt-6 text-center text-sm text-gray-400">
        © 2026 AI Companion. All rights reserved.
      </div>

    </footer>
  );
}