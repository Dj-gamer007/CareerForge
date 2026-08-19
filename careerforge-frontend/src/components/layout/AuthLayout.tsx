import { Outlet, Link } from 'react-router-dom';
import { Briefcase } from 'lucide-react';

export function AuthLayout() {
  return (
    <div className="min-h-screen flex flex-col justify-center py-12 sm:px-6 lg:px-8 bg-slate-50">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center mb-6">
        <Link to="/" className="inline-flex items-center gap-2 font-bold text-2xl text-slate-900">
          <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center text-white shadow-md shadow-indigo-200">
            <Briefcase className="w-6 h-6" />
          </div>
          <span>Career<span className="text-indigo-600">Forge</span></span>
        </Link>
      </div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-white py-8 px-6 shadow-xl shadow-slate-200/50 rounded-2xl border border-slate-200/80 sm:px-10">
          <Outlet />
        </div>
      </div>
    </div>
  );
}
