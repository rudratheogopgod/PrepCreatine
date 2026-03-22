import { useState } from 'react'
import { motion } from 'framer-motion'
import { Star, Zap, ShieldAlert, Heart } from 'lucide-react'
import Button from '../../components/ui/Button'

// Simulation of game logic
export default function Game() {
  const [level, setLevel] = useState(1)
  const [health, setHealth] = useState(3)
  const [score, setScore] = useState(0)
  const [inCombat, setInCombat] = useState(true)

  const currentMonster = { name: 'Integration Boss', hp: 100 }

  const handleAttack = () => {
    setScore(s => s + 25)
    if (score > 100) {
      setInCombat(false)
      setLevel(2)
    }
  }

  return (
    <div className="h-screen w-screen bg-slate-950 flex flex-col items-center justify-center p-4 relative overflow-hidden font-mono">
      {/* HUD */}
      <div className="absolute top-6 left-6 flex gap-4">
        <div className="bg-slate-800/80 p-3 rounded-xl border border-slate-700 flex items-center gap-2 text-white">
          <Heart className="text-red-500" fill="currentColor" /> {health}
        </div>
        <div className="bg-slate-800/80 p-3 rounded-xl border border-slate-700 flex items-center gap-2 text-white">
          <Star className="text-amber-400" fill="currentColor" /> Score: {score}
        </div>
      </div>

      <div className="absolute top-6 right-6">
        <div className="bg-slate-800/80 px-4 py-2 rounded-xl border border-slate-700 text-white font-bold tracking-widest">
          LVL {level}
        </div>
      </div>

      {inCombat ? (
        <div className="w-full max-w-lg text-center">
          <motion.div
            animate={{ y: [0, -20, 0] }}
            transition={{ repeat: Infinity, duration: 2 }}
            className="w-48 h-48 bg-purple-900/50 rounded-full mx-auto mb-8 border-4 border-purple-500 flex items-center justify-center shadow-[0_0_50px_rgba(168,85,247,0.5)]"
          >
            <ShieldAlert size={80} className="text-purple-400" />
          </motion.div>

          <h2 className="text-3xl text-white font-bold mb-2">{currentMonster.name}</h2>
          <div className="w-full h-4 bg-slate-800 rounded-full mb-12 overflow-hidden border border-slate-700">
            <div className="h-full bg-red-500" style={{ width: `${Math.max(0, 100 - score)}%` }} />
          </div>

          <div className="bg-slate-800 p-6 rounded-2xl border border-slate-700">
            <p className="text-slate-300 mb-6 text-lg">Evaluate the integral: ∫ e^(2x) dx</p>
            <div className="grid grid-cols-2 gap-4">
              <Button variant="secondary" onClick={() => { setHealth(h=>h-1) }} className="dark:hover:bg-slate-700 justify-start">A) e^(2x) + c</Button>
              <Button variant="secondary" onClick={handleAttack} className="dark:hover:bg-slate-700 border-sky-500 text-sky-400 justify-start">B) (1/2)e^(2x) + c</Button>
              <Button variant="secondary" onClick={() => { setHealth(h=>h-1) }} className="dark:hover:bg-slate-700 justify-start">C) 2e^(2x) + c</Button>
              <Button variant="secondary" onClick={() => { setHealth(h=>h-1) }} className="dark:hover:bg-slate-700 justify-start">D) 0</Button>
            </div>
          </div>
        </div>
      ) : (
        <div className="text-center">
          <div className="w-32 h-32 bg-green-500/20 rounded-full mx-auto mb-6 flex items-center justify-center border-4 border-green-500">
             <Zap size={60} className="text-green-400" />
          </div>
          <h2 className="text-4xl text-white font-bold mb-4">Enemy Defeated!</h2>
          <p className="text-slate-400 mb-8">+100 EXP</p>
          <Button onClick={() => setInCombat(true)}>Next Level</Button>
        </div>
      )}.
    </div>
  )
}
