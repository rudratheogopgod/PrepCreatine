import React from 'react';

const DemoBanner = () => {
  return (
    <div className="fixed bottom-0 left-0 w-full bg-blue-600 text-white py-2 px-4 flex justify-between items-center z-50 text-sm shadow-lg">
      <div className="flex items-center gap-2">
        <span className="bg-white text-blue-600 px-2 py-0.5 rounded-full font-bold text-xs">DEMO</span>
        <span>Viewing as <b>Arjun Sharma</b> (Class 12 - JEE Prep)</span>
      </div>
      <button 
        onClick={() => alert("Demo data reset!")}
        className="underline hover:text-blue-100 transition-colors"
      >
        Reset Demo State
      </button>
    </div>
  );
};

export default DemoBanner;