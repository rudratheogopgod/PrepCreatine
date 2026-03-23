// Is file mein routes update karein
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Planner from './pages/ui/Planner';
import Knowledge from './pages/ui/Knowledge';
import DemoBanner from './components/demo/DemoBanner';
import Navbar from './components/layout/Navbar';
import Footer from './components/layout/Footer';
import Home from "../src/pages/Home/Home"
import Dashboard from './pages/ui/Dashboard';
import Learn from './pages/ui/Learn';
import Test from './pages/ui/Test';
import Progress from './pages/ui/Progress'

function App() {
  return (

    <div className="min-h-screen bg-gray-50 pb-12"> {/* pb-12 for demo banner space */}
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/planner" element={<Planner />} />
        <Route path="/knowledge" element={<Knowledge />} />
        <Route path="/learn" element={<Learn />} />
        <Route path="/test" element={<Test />} />
        <Route path="/Progress" element={<Progress/>} />
      </Routes>
      <Footer />
      <DemoBanner />
    </div>

  );
}
export default App;