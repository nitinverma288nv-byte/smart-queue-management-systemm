import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';
import { 
  Activity, 
  Building, 
  BookOpen, 
  Clock, 
  Users, 
  User as UserIcon, 
  Bell, 
  LogOut, 
  ArrowRight, 
  CheckCircle, 
  AlertTriangle, 
  UserCheck, 
  Settings, 
  Plus, 
  Trash, 
  ChevronRight, 
  Shield, 
  FileText,
  Calendar,
  AlertOctagon,
  RefreshCw
} from 'lucide-react';

const API_BASE = 'http://localhost:9999/api';

// Create Global Auth Context
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('sq_user');
    return stored ? JSON.parse(stored) : null;
  });

  const login = (userData) => {
    setUser(userData);
    localStorage.setItem('sq_user', JSON.stringify(userData));
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('sq_user');
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);

// Main React App
export default function App() {
  return (
    <AuthProvider>
      <MainLayout />
    </AuthProvider>
  );
}

function MainLayout() {
  const { user, logout } = useAuth();
  const [page, setPage] = useState('landing'); // landing, login, register, dashboard, hospital, bank, college, staff, admin
  const [loginType, setLoginType] = useState('user'); // user, staff, admin
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfileDropdown, setShowProfileDropdown] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [activeToken, setActiveToken] = useState(null);
  const [tokenPosition, setTokenPosition] = useState(null);
  const [activeTokens, setActiveTokens] = useState([]);
  const [tokenPositions, setTokenPositions] = useState({});
  const [resetToken, setResetToken] = useState(null);

  // Parse URL parameters for reset token on initialization
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    if (token) {
      setResetToken(token);
      setPage('reset-password');
    }
  }, []);

  // Short polling for user notifications & active token position
  useEffect(() => {
    if (!user) return;

    const fetchData = async () => {
      try {
        // Fetch notifications
        const notifRes = await axios.get(`${API_BASE}/notification/user/${user.userId}`, {
          headers: { Authorization: `Bearer ${user.token}` }
        });
        if (notifRes.data.success) {
          setNotifications(notifRes.data.data);
        }

        // Fetch active tokens
        const tokenRes = await axios.get(`${API_BASE}/token/user/${user.userId}`, {
          headers: { Authorization: `Bearer ${user.token}` }
        });
        if (tokenRes.data.success && tokenRes.data.data.length > 0) {
          const actives = tokenRes.data.data.filter(t => t.status === 'WAITING' || t.status === 'SERVING');
          setActiveTokens(actives);
          
          // Backwards compatibility: first active token
          const active = actives[0] || null;
          setActiveToken(active);

          const positions = {};
          for (const act of actives) {
            try {
              const posRes = await axios.get(`${API_BASE}/token/${act.id}/position`, {
                headers: { Authorization: `Bearer ${user.token}` }
              });
              if (posRes.data.success) {
                positions[act.id] = posRes.data.data;
              }
            } catch (err) {
              console.error("Error fetching token position:", act.id, err);
            }
          }
          setTokenPositions(positions);

          if (active && positions[active.id]) {
            setTokenPosition(positions[active.id]);
          } else {
            setTokenPosition(null);
          }
        } else {
          setActiveTokens([]);
          setTokenPositions({});
          setActiveToken(null);
          setTokenPosition(null);
        }
      } catch (err) {
        console.error("Short polling error:", err);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, 4000);
    return () => clearInterval(interval);
  }, [user]);

  // Route protection / redirection
  useEffect(() => {
    if (user) {
      if (user.role === 'ROLE_ADMIN') {
        setPage('admin');
      } else if (user.role === 'ROLE_STAFF') {
        setPage('staff');
      } else {
        if (page === 'landing' || page === 'login' || page === 'register' || page === 'forgot-password' || page === 'reset-password') {
          setPage('dashboard');
        }
      }
    } else {
      if (page !== 'login' && page !== 'register' && page !== 'forgot-password' && page !== 'reset-password') {
        setPage('landing');
      }
    }
  }, [user]);

  const handleLogout = () => {
    logout();
    setPage('landing');
  };

  const markAllRead = async () => {
    try {
      await axios.post(`${API_BASE}/notification/user/${user.userId}/read-all`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      setNotifications(notifications.map(n => ({ ...n, isRead: true })));
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="min-h-screen bg-[#0b0f19] text-slate-100 flex flex-col font-sans">
      {/* Premium Navbar */}
      <nav className="glass-panel sticky top-0 z-40 px-6 py-4 flex items-center justify-between shadow-lg">
        <div className="flex items-center space-x-3 cursor-pointer" onClick={() => setPage(user ? (user.role === 'ROLE_ADMIN' ? 'admin' : (user.role === 'ROLE_STAFF' ? 'staff' : 'dashboard')) : 'landing')}>
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center shadow-md shadow-blue-500/20">
            <Clock className="w-5 h-5 text-white" />
          </div>
          <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-white to-slate-400 bg-clip-text text-transparent uppercase">
            SMART QUEUE MANAGEMENT SYSTEM
          </span>
        </div>

        <div className="flex items-center space-x-6">
          {user ? (
            <>
              {user.role === 'ROLE_USER' && (
                <button 
                  onClick={() => setPage('dashboard')} 
                  className={`text-sm font-medium transition-colors ${page === 'dashboard' ? 'text-blue-500' : 'text-slate-300 hover:text-white'}`}
                >
                  Dashboard
                </button>
              )}
              
              {/* Notification bell */}
              <div className="relative">
                <button 
                  onClick={() => setShowNotifications(!showNotifications)} 
                  className="p-2 rounded-lg bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700/50 transition-colors relative"
                >
                  <Bell className="w-4 h-4 text-slate-300" />
                  {notifications.some(n => !n.isRead) && (
                    <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-blue-500 rounded-full animate-pulse"></span>
                  )}
                </button>

                {showNotifications && (
                  <div className="absolute right-0 mt-3 w-80 max-h-96 overflow-y-auto rounded-xl glass-panel border border-slate-700 shadow-2xl p-4 z-50">
                    <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                      <span className="font-semibold text-sm">Notifications</span>
                      <button onClick={markAllRead} className="text-xs text-blue-500 hover:underline">Mark all read</button>
                    </div>
                    <div className="mt-3 space-y-2">
                      {notifications.length === 0 ? (
                        <p className="text-xs text-slate-500 text-center py-4">No notifications yet</p>
                      ) : (
                        notifications.map(notif => (
                          <div key={notif.id} className={`p-2.5 rounded-lg text-xs border ${notif.isRead ? 'bg-slate-900/40 border-slate-800 text-slate-400' : 'bg-blue-950/20 border-blue-900/40 text-slate-200'}`}>
                            {notif.message}
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* Interactive User Dropdown */}
              <div className="relative">
                <button 
                  onClick={() => setShowProfileDropdown(!showProfileDropdown)}
                  className="flex items-center space-x-3 bg-slate-800/60 pl-3 pr-4 py-1.5 rounded-xl border border-slate-700/50 hover:bg-slate-700/50 transition-colors text-left focus:outline-none cursor-pointer"
                >
                  {user.profileImage ? (
                    <img src={user.profileImage} alt="Avatar" className="w-7 h-7 rounded-lg object-cover border border-slate-600/50" />
                  ) : (
                    <div className="w-7 h-7 rounded-lg bg-blue-600/20 text-blue-400 flex items-center justify-center font-bold text-xs uppercase">
                      {user.fullName.substring(0, 2)}
                    </div>
                  )}
                  <div>
                    <div className="text-xs font-semibold leading-3 text-slate-200">{user.fullName}</div>
                    <div className="text-[10px] text-slate-400 capitalize">{user.role.replace('ROLE_', '').toLowerCase()}</div>
                  </div>
                </button>

                {showProfileDropdown && (
                  <div className="absolute right-0 mt-3 w-48 rounded-xl glass-panel border border-slate-700 shadow-2xl p-2 z-50 animate-in fade-in slide-in-from-top-2 duration-150">
                    <button 
                      onClick={() => { setShowProfileDropdown(false); setPage('profile'); }}
                      className="w-full flex items-center space-x-2.5 px-3 py-2 text-xs font-medium text-slate-200 hover:bg-slate-800/60 rounded-lg transition-colors cursor-pointer"
                    >
                      <UserIcon className="w-4 h-4 text-blue-400" />
                      <span>My Profile</span>
                    </button>
                    
                    <button 
                      onClick={() => { setShowProfileDropdown(false); setPage('profile'); }}
                      className="w-full flex items-center space-x-2.5 px-3 py-2 text-xs font-medium text-slate-200 hover:bg-slate-800/60 rounded-lg transition-colors cursor-pointer"
                    >
                      <Settings className="w-4 h-4 text-indigo-400" />
                      <span>Settings</span>
                    </button>

                    <div className="my-1 border-t border-slate-800/80"></div>

                    <button 
                      onClick={() => { setShowProfileDropdown(false); handleLogout(); }}
                      className="w-full flex items-center space-x-2.5 px-3 py-2 text-xs font-medium text-rose-400 hover:bg-rose-500/10 rounded-lg transition-colors cursor-pointer"
                    >
                      <LogOut className="w-4 h-4" />
                      <span>Logout</span>
                    </button>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="flex items-center space-x-4">
              <button 
                onClick={() => setPage('login')} 
                className="text-sm font-medium text-slate-300 hover:text-white transition-colors"
              >
                Login
              </button>
              <button 
                onClick={() => setPage('register')} 
                className="px-4 py-2 text-sm font-medium rounded-xl bg-blue-600 hover:bg-blue-500 text-white shadow-md shadow-blue-500/20 transition-all hover:scale-105"
              >
                Register
              </button>
            </div>
          )}
        </div>
      </nav>

      {/* Pages Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-6 md:p-8">
        {page === 'landing' && <LandingPage setPage={setPage} setLoginType={setLoginType} />}
        {page === 'login' && <LoginPage setPage={setPage} loginType={loginType} setLoginType={setLoginType} />}
        {page === 'register' && <RegisterPage setPage={setPage} />}
        {page === 'forgot-password' && <ForgotPasswordPage setPage={setPage} />}
        {page === 'reset-password' && <ResetPasswordPage setPage={setPage} resetToken={resetToken} />}
        {page === 'dashboard' && (
          <UserDashboard 
            setPage={setPage} 
            activeToken={activeToken} 
            tokenPosition={tokenPosition} 
            setActiveToken={setActiveToken}
            activeTokens={activeTokens}
            tokenPositions={tokenPositions}
          />
        )}
        {page === 'hospital' && <HospitalModule setPage={setPage} />}
        {page === 'bank' && <BankModule setPage={setPage} />}
        {page === 'college' && <CollegeModule setPage={setPage} />}
        {page === 'staff' && <StaffDashboard />}
        {page === 'admin' && <AdminDashboard />}
        {page === 'profile' && <ProfilePage setPage={setPage} />}
      </main>

      {/* Footer */}
      <footer className="py-6 border-t border-slate-900 bg-slate-950/40 text-center text-xs text-slate-500">
        &copy; {new Date().getFullYear()} Smart Queue Management System. Premium Real-Time Performance Engine.
      </footer>
    </div>
  );
}

// ----------------------------------------------------
// 1. LANDING PAGE
// ----------------------------------------------------
function LandingPage({ setPage, setLoginType }) {
  return (
    <div className="py-12 flex flex-col items-center justify-center space-y-12">
      <div className="text-center max-w-2xl mx-auto space-y-4">
        <div className="inline-flex items-center space-x-2 px-3.5 py-1.5 rounded-full bg-blue-600/10 border border-blue-500/20 text-xs font-semibold text-blue-400">
          <Activity className="w-3.5 h-3.5" />
          <span>Real-time Dynamic Queue Management System</span>
        </div>
        <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight leading-tight">
          SMART QUEUE <br/>
          <span className="bg-gradient-to-r from-blue-500 to-indigo-400 bg-clip-text text-transparent uppercase">
            MANAGEMENT SYSTEM
          </span>
        </h1>
        <p className="text-slate-400 text-sm md:text-base">
          Choose your access portal to proceed to real-time slot booking, live token tracking, and department handling.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 w-full max-w-4xl px-4">
        {/* User Card */}
        <div 
          onClick={() => { setLoginType('user'); setPage('login'); }}
          className="glass-panel hover:bg-slate-900/40 rounded-2xl p-6 border border-slate-800 hover:border-blue-500/40 cursor-pointer flex flex-col items-center text-center space-y-4 transition-all duration-300 hover:scale-105"
        >
          <div className="w-16 h-16 rounded-2xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center shadow-lg shadow-blue-500/5">
            <Users className="w-8 h-8 text-blue-400" />
          </div>
          <h3 className="text-xl font-bold text-slate-100">User Login</h3>
          <p className="text-slate-400 text-xs leading-relaxed">
            Generate tokens, check live queue positions, book slots, and make digital payments.
          </p>
        </div>

        {/* Staff Card */}
        <div 
          onClick={() => { setLoginType('staff'); setPage('login'); }}
          className="glass-panel hover:bg-slate-900/40 rounded-2xl p-6 border border-slate-800 hover:border-emerald-500/40 cursor-pointer flex flex-col items-center text-center space-y-4 transition-all duration-300 hover:scale-105"
        >
          <div className="w-16 h-16 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center shadow-lg shadow-emerald-500/5">
            <UserCheck className="w-8 h-8 text-emerald-400" />
          </div>
          <h3 className="text-xl font-bold text-slate-100">Staff Login</h3>
          <p className="text-slate-400 text-xs leading-relaxed">
            Call next tickets, complete tokens, handle emergencies, and configure workstations.
          </p>
        </div>

        {/* Admin Card */}
        <div 
          onClick={() => { setLoginType('admin'); setPage('login'); }}
          className="glass-panel hover:bg-slate-900/40 rounded-2xl p-6 border border-slate-800 hover:border-violet-500/40 cursor-pointer flex flex-col items-center text-center space-y-4 transition-all duration-300 hover:scale-105"
        >
          <div className="w-16 h-16 rounded-2xl bg-violet-500/10 border border-violet-500/20 flex items-center justify-center shadow-lg shadow-violet-500/5">
            <Shield className="w-8 h-8 text-violet-400" />
          </div>
          <h3 className="text-xl font-bold text-slate-100">Admin Login</h3>
          <p className="text-slate-400 text-xs leading-relaxed">
            Access system diagnostics, dynamic analytics, manage staff and departments.
          </p>
        </div>
      </div>
    </div>
  );
}

// ----------------------------------------------------
// 2. LOGIN PAGE
// ----------------------------------------------------
function LoginPage({ setPage, loginType, setLoginType }) {
  const { login } = useAuth();
  const [form, setForm] = useState({ username: '', password: '' });

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  // 2-Step Staff Login states
  const [tempStaffUser, setTempStaffUser] = useState(null);
  const [staffProfile, setStaffProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [customDeskMode, setCustomDeskMode] = useState(false);

  // Cascading Selector states
  const [deskSector, setDeskSector] = useState('');
  const [deskOrgs, setDeskOrgs] = useState([]);
  const [selectedDeskOrgId, setSelectedDeskOrgId] = useState('');
  const [deskBranches, setDeskBranches] = useState([]);
  const [selectedDeskBranchId, setSelectedDeskBranchId] = useState('');
  const [deskCounters, setDeskCounters] = useState([]);
  const [selectedDeskCounterId, setSelectedDeskCounterId] = useState('');
  const [updatingCounter, setUpdatingCounter] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const res = await axios.post(`${API_BASE}/auth/login`, form);
      if (res.data.success) {
        const userData = res.data.data;
        login(userData);
      }
    } catch (err) {
      if (err.response) {
        const data = err.response.data;
        setError(data?.message || 'Incorrect Username or Password');
      } else if (err.request) {
        setError("Network error! Server did not respond. Check if backend is running on port 9999.");
      } else {
        setError('Incorrect Username or Password');
      }
    } finally {
      setLoading(false);
    }
  };

  // Fetch organisations when Sector changes
  useEffect(() => {
    if (!tempStaffUser || !deskSector) return;
    setSelectedDeskOrgId('');
    setSelectedDeskBranchId('');
    setSelectedDeskCounterId('');
    setDeskOrgs([]);
    setDeskBranches([]);
    setDeskCounters([]);

    const endpoint = deskSector === 'HOSPITAL' ? '/hospital/list' : (deskSector === 'BANK' ? '/bank/list' : '/college/list');
    axios.get(`${API_BASE}${endpoint}`, {
      headers: { Authorization: `Bearer ${tempStaffUser.token}` }
    }).then(res => {
      if (res.data.success) {
        setDeskOrgs(res.data.data);
      }
    }).catch(err => console.error(err));
  }, [deskSector, tempStaffUser]);

  // Fetch branches when Organisation changes
  useEffect(() => {
    if (!tempStaffUser || !selectedDeskOrgId) return;
    setSelectedDeskBranchId('');
    setSelectedDeskCounterId('');
    setDeskBranches([]);
    setDeskCounters([]);

    const endpoint = deskSector === 'HOSPITAL' ? `/hospital/${selectedDeskOrgId}/branches` : (deskSector === 'BANK' ? `/bank/${selectedDeskOrgId}/branches` : `/college/${selectedDeskOrgId}/departments`);
    axios.get(`${API_BASE}${endpoint}`, {
      headers: { Authorization: `Bearer ${tempStaffUser.token}` }
    }).then(res => {
      if (res.data.success) {
        setDeskBranches(res.data.data);
      }
    }).catch(err => console.error(err));
  }, [selectedDeskOrgId, tempStaffUser]);

  // Fetch counters when Branch changes
  useEffect(() => {
    if (!tempStaffUser || !selectedDeskBranchId) return;
    setSelectedDeskCounterId('');
    setDeskCounters([]);

    axios.get(`${API_BASE}/staff/counters?sectorType=${deskSector}&branchId=${selectedDeskBranchId}`, {
      headers: { Authorization: `Bearer ${tempStaffUser.token}` }
    }).then(res => {
      if (res.data.success) {
        setDeskCounters(res.data.data);
      }
    }).catch(err => console.error(err));
  }, [selectedDeskBranchId, tempStaffUser]);

  const handleKeepCurrent = () => {
    login(tempStaffUser);
  };

  const handleUpdateDeskAndLogin = async (e) => {
    e.preventDefault();
    if (!deskSector || !selectedDeskBranchId || !selectedDeskCounterId || !tempStaffUser) return;
    setUpdatingCounter(true);
    setError('');

    try {
      const res = await axios.post(`${API_BASE}/staff/update-counter?sectorType=${deskSector}&referenceId=${selectedDeskBranchId}&counterId=${selectedDeskCounterId}`, {}, {
        headers: { Authorization: `Bearer ${tempStaffUser.token}` }
      });
      if (res.data.success) {
        login(tempStaffUser);
      }
    } catch (err) {
      console.error(err);
      setError("Failed to update workstation/desk. Please try again.");
    } finally {
      setUpdatingCounter(false);
    }
  };

  // If tempStaffUser is set, render Step 2: Staff Session Setup
  if (tempStaffUser) {
    return (
      <div className="max-w-md w-full mx-auto py-12">
        <div className="glass-panel border border-slate-800 rounded-2xl p-8 shadow-2xl space-y-6">
          <div className="text-center space-y-2">
            <h2 className="text-2xl font-bold flex items-center justify-center gap-2">
              <Shield className="w-6 h-6 text-blue-500" />
              Staff Desk Setup
            </h2>
            <p className="text-xs text-slate-400">Welcome, {tempStaffUser.fullName}! Set your workstation for this session.</p>
          </div>

          {error && (
            <div className="p-3.5 bg-rose-500/10 border border-rose-500/20 text-rose-400 rounded-xl text-xs flex items-center space-x-2">
              <AlertTriangle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {profileLoading ? (
            <div className="flex flex-col items-center justify-center py-12 space-y-3">
              <RefreshCw className="w-8 h-8 text-blue-500 animate-spin" />
              <span className="text-xs text-slate-400">Retrieving profile and workstations...</span>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Profile details or setup */}
              {!customDeskMode && staffProfile?.counter ? (
                <div className="space-y-6">
                  <div className="p-5 bg-slate-900/80 border border-slate-800 rounded-2xl space-y-3 shadow-inner">
                    <span className="text-[10px] font-bold text-blue-500 uppercase tracking-widest block">Active Workstation</span>
                    <div className="space-y-1">
                      <div className="text-base font-extrabold text-slate-100 flex items-center gap-2">
                        <Building className="w-4.5 h-4.5 text-slate-400" />
                        {staffProfile.counter.counterName || staffProfile.counter.name || 'Assigned Counter'}
                      </div>
                      <div className="text-xs text-slate-400 capitalize">
                        Sector: {staffProfile.sectorType.toLowerCase()} | Desk Status: {staffProfile.counter.status || 'ACTIVE'}
                      </div>
                    </div>
                  </div>

                  <div className="space-y-3">
                    <button 
                      onClick={handleKeepCurrent}
                      className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-xl transition-all shadow-md shadow-blue-500/20 flex items-center justify-center gap-2 animate-pulse-subtle"
                    >
                      Keep Current Desk & Sign In
                      <ArrowRight className="w-4 h-4" />
                    </button>

                    <button 
                      onClick={() => setCustomDeskMode(true)}
                      className="w-full py-3 bg-slate-800/80 hover:bg-slate-800 border border-slate-700/50 text-slate-300 text-xs font-bold rounded-xl transition-all"
                    >
                      Switch Desk / Counter
                    </button>
                  </div>
                </div>
              ) : (
                <form onSubmit={handleUpdateDeskAndLogin} className="space-y-4">
                  <div className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">Select Active Desk</div>

                  {/* 1. Sector */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-400">Sector</label>
                    <select
                      value={deskSector}
                      onChange={e => setDeskSector(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-blue-500 transition-colors"
                      required
                    >
                      <option value="">Select Sector</option>
                      <option value="HOSPITAL">Hospital</option>
                      <option value="BANK">Bank</option>
                      <option value="COLLEGE">College</option>
                    </select>
                  </div>

                  {/* 2. Organisation */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-400">Organisation</label>
                    <select
                      value={selectedDeskOrgId}
                      onChange={e => setSelectedDeskOrgId(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-blue-500 transition-colors"
                      disabled={!deskSector}
                      required
                    >
                      <option value="">Select Organisation</option>
                      {deskOrgs.map(org => (
                        <option key={org.id} value={org.id}>{org.name}</option>
                      ))}
                    </select>
                  </div>

                  {/* 3. Branch / Department */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-400">
                      {deskSector === 'COLLEGE' ? 'Department' : 'Branch'}
                    </label>
                    <select
                      value={selectedDeskBranchId}
                      onChange={e => setSelectedDeskBranchId(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-blue-500 transition-colors"
                      disabled={!selectedDeskOrgId}
                      required
                    >
                      <option value="">
                        {deskSector === 'COLLEGE' ? 'Select Department' : 'Select Branch'}
                      </option>
                      {deskBranches.map(br => (
                        <option key={br.id} value={br.id}>{br.name || br.branchName || br.deptName}</option>
                      ))}
                    </select>
                  </div>

                  {/* 4. Counter */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-400">Counter / Desk / Doctor</label>
                    <select
                      value={selectedDeskCounterId}
                      onChange={e => setSelectedDeskCounterId(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-3 text-sm focus:outline-none focus:border-blue-500 transition-colors"
                      disabled={!selectedDeskBranchId}
                      required
                    >
                      <option value="">Select Counter</option>
                      {deskCounters.map(c => (
                        <option key={c.id} value={c.id}>{c.counterName || c.name || `Counter ${c.counterNumber}`}</option>
                      ))}
                    </select>
                  </div>

                  <div className="pt-2 space-y-3">
                    <button 
                      type="submit"
                      disabled={!deskSector || !selectedDeskBranchId || !selectedDeskCounterId || updatingCounter}
                      className="w-full py-3 bg-blue-600 hover:bg-blue-500 disabled:bg-slate-800 disabled:text-slate-500 disabled:cursor-not-allowed text-white font-semibold rounded-xl transition-all shadow-md shadow-blue-500/20 flex items-center justify-center gap-2"
                    >
                      {updatingCounter ? (
                        <>
                          <RefreshCw className="w-4 h-4 animate-spin" />
                          Updating Workspace...
                        </>
                      ) : (
                        <>
                          Confirm Workspace & Sign In
                          <ArrowRight className="w-4 h-4" />
                        </>
                      )}
                    </button>

                    {staffProfile?.counter && (
                      <button 
                        type="button"
                        onClick={() => setCustomDeskMode(false)}
                        className="w-full py-2 text-slate-400 hover:text-white text-xs transition-colors text-center font-bold"
                      >
                        Cancel & Use Current Desk
                      </button>
                    )}
                  </div>
                </form>
              )}
            </div>
          )}

          <div className="text-center pt-2">
            <button 
              onClick={() => {
                setTempStaffUser(null);
                setForm({ username: '', password: '' });
              }} 
              className="text-blue-500 hover:underline text-xs"
            >
              Sign in with another account
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-md w-full mx-auto py-12">
      <div className="glass-panel border border-slate-800 rounded-2xl p-8 shadow-2xl space-y-6">
        <div className="text-center space-y-2">
          <h2 className="text-2xl font-bold text-slate-100">
            {loginType === 'admin' ? 'Admin Portal Sign In' : loginType === 'staff' ? 'Staff Console Sign In' : 'User Sign In'}
          </h2>
          <p className="text-xs text-slate-400">
            {loginType === 'admin' ? 'Locked to sole system administrator credentials' : 'Enter credentials to access your session'}
          </p>
        </div>

        {error && (
          <div className="p-3.5 bg-rose-500/10 border border-rose-500/20 text-rose-400 rounded-xl text-xs flex items-center space-x-2">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {loginType === 'staff' && (
            <div className="p-3 bg-blue-500/10 border border-blue-500/20 rounded-xl flex items-center gap-2">
              <svg className="w-4 h-4 text-blue-400 shrink-0" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
              <span className="text-[11px] text-blue-300">Your sector (Hospital / Bank / College) is automatically detected from your username.</span>
            </div>
          )}

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Username</label>
            <input 
              type="text" 
              required
              value={form.username}
              onChange={e => setForm({ ...form, username: e.target.value })}
              placeholder={loginType === 'admin' ? 'e.g. _Pankaj_03' : 'Enter username'} 
              className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl px-4 py-3 text-sm focus:outline-none text-slate-200 transition-colors"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Password</label>
            <div className="relative">
              <input 
                type={showPassword ? "text" : "password"} 
                required
                value={form.password}
                onChange={e => setForm({ ...form, password: e.target.value })}
                placeholder="••••••••" 
                className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl pl-4 pr-12 py-3 text-sm focus:outline-none text-slate-200 transition-colors"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors focus:outline-none cursor-pointer"
              >
                {showPassword ? (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.822 7.822L21 21m-3.228-3.228l-3.65-3.65M12 9.75a2.25 2.25 0 103.5 3.5m-3.5-3.5L12.75 12"></path>
                  </svg>
                ) : (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"></path>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                  </svg>
                )}
              </button>
            </div>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white font-medium rounded-xl transition-all shadow-md shadow-blue-500/20 cursor-pointer"
          >
            {loading ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>

        <div className="text-center text-xs text-slate-400 space-y-2">
          {loginType === 'user' && (
            <>
              <div>
                <button onClick={() => setPage('forgot-password')} className="text-blue-500 hover:underline cursor-pointer">Forgot Password?</button>
              </div>
              <div>
                <span>Don't have an account? </span>
                <button onClick={() => setPage('register')} className="text-blue-500 hover:underline cursor-pointer">Register now</button>
              </div>
            </>
          )}
          <div>
            <button onClick={() => setPage('landing')} className="text-slate-500 hover:underline cursor-pointer">Back to Portals</button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ----------------------------------------------------
// 3. REGISTER PAGE
// ----------------------------------------------------
function RegisterPage({ setPage }) {
  const { login } = useAuth();
  const [form, setForm] = useState({ username: '', password: '', email: '', fullName: '', role: 'USER' });
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (form.password !== confirmPassword) {
      setError('Passwords do not match!');
      return;
    }
    if (form.password.length < 6) {
      setError('Password must be at least 6 characters!');
      return;
    }

    setLoading(true);

    try {
      const res = await axios.post(`${API_BASE}/auth/register`, { ...form, role: 'USER' });
      if (res.data.success) {
        login(res.data.data);
      }
    } catch (err) {
      if (err.response) {
        const data = err.response.data;
        setError(data?.message || `Registration failed (${err.response.status})`);
      } else if (err.request) {
        setError("Network error! Server did not respond. Check if backend is running on port 9999.");
      } else {
        setError(err.message || 'Error occurred during registration!');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md w-full mx-auto py-6">
      <div className="glass-panel border border-slate-800 rounded-2xl p-8 shadow-2xl space-y-6">
        <div className="text-center space-y-2">
          <h2 className="text-2xl font-bold">Create Account</h2>
          <p className="text-xs text-slate-400">Register as a user to book queue tokens</p>
        </div>

        {/* Admin/Staff blocked notice */}
        <div className="p-3 bg-amber-500/5 border border-amber-500/20 rounded-xl text-[11px] text-amber-400 flex items-start space-x-2">
          <span className="text-amber-400 mt-0.5">ℹ️</span>
          <span>Administrator accounts are managed securely by the system owner. Staff accounts are created by administrators only.</span>
        </div>

        {error && (
          <div className="p-3.5 bg-rose-500/10 border border-rose-500/20 text-rose-400 rounded-xl text-xs flex items-center space-x-2">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Full Name</label>
            <input 
              type="text" 
              required
              value={form.fullName}
              onChange={e => setForm({ ...form, fullName: e.target.value })}
              placeholder="e.g. John Doe" 
              className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl px-4 py-3 text-sm focus:outline-none transition-colors"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Email Address</label>
            <input 
              type="email" 
              required
              value={form.email}
              onChange={e => setForm({ ...form, email: e.target.value })}
              placeholder="johndoe@email.com" 
              className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl px-4 py-3 text-sm focus:outline-none transition-colors"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Username</label>
            <input 
              type="text" 
              required
              value={form.username}
              onChange={e => setForm({ ...form, username: e.target.value })}
              placeholder="Choose unique username" 
              className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl px-4 py-3 text-sm focus:outline-none transition-colors"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Password</label>
            <div className="relative">
              <input 
                type={showPassword ? "text" : "password"} 
                required
                minLength={6}
                value={form.password}
                onChange={e => setForm({ ...form, password: e.target.value })}
                placeholder="••••••••" 
                className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl pl-4 pr-12 py-3 text-sm focus:outline-none transition-colors"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors focus:outline-none cursor-pointer"
              >
                {showPassword ? (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.822 7.822L21 21m-3.228-3.228l-3.65-3.65M12 9.75a2.25 2.25 0 103.5 3.5m-3.5-3.5L12.75 12"></path>
                  </svg>
                ) : (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"></path>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                  </svg>
                )}
              </button>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Confirm Password</label>
            <div className="relative">
              <input 
                type={showConfirmPassword ? "text" : "password"} 
                required
                minLength={6}
                value={confirmPassword}
                onChange={e => setConfirmPassword(e.target.value)}
                placeholder="••••••••" 
                className={`w-full bg-slate-900 border focus:border-blue-500 rounded-xl pl-4 pr-12 py-3 text-sm focus:outline-none transition-colors ${
                  confirmPassword && form.password !== confirmPassword 
                    ? 'border-rose-500/60' 
                    : 'border-slate-800'
                }`}
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors focus:outline-none cursor-pointer"
              >
                {showConfirmPassword ? (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.822 7.822L21 21m-3.228-3.228l-3.65-3.65M12 9.75a2.25 2.25 0 103.5 3.5m-3.5-3.5L12.75 12"></path>
                  </svg>
                ) : (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"></path>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                  </svg>
                )}
              </button>
            </div>
            {confirmPassword && form.password !== confirmPassword && (
              <p className="text-[10px] text-rose-400 mt-1">Passwords do not match</p>
            )}
          </div>

          <button 
            type="submit" 
            disabled={loading || (confirmPassword && form.password !== confirmPassword)}
            className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white font-medium rounded-xl transition-all shadow-md shadow-blue-500/20 disabled:opacity-50 cursor-pointer"
          >
            {loading ? 'Registering...' : 'Create Account'}
          </button>
        </form>

        <div className="text-center text-xs text-slate-400">
          <span>Already have an account? </span>
          <button onClick={() => setPage('login')} className="text-blue-500 hover:underline cursor-pointer">Login here</button>
        </div>
      </div>
    </div>
  );
}

// ----------------------------------------------------
// 3a. FORGOT PASSWORD PAGE
// ----------------------------------------------------
function ForgotPasswordPage({ setPage }) {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');

    try {
      const res = await axios.post(`${API_BASE}/auth/forgot-password`, { email });
      if (res.data.success) {
        setMessage(res.data.message || 'Password reset link sent! Check your inbox or console.');
      }
    } catch (err) {
      if (err.response) {
        setError(err.response.data?.message || 'Failed to send password reset link!');
      } else {
        setError('Network error! Please try again later.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md w-full mx-auto py-12">
      <div className="glass-panel border border-slate-800 rounded-2xl p-8 shadow-2xl space-y-6">
        <div className="text-center space-y-2">
          <h2 className="text-2xl font-bold">Forgot Password</h2>
          <p className="text-xs text-slate-400">Enter your registered email address to receive a secure reset link</p>
        </div>

        {error && (
          <div className="p-3.5 bg-rose-500/10 border border-rose-500/20 text-rose-400 rounded-xl text-xs flex items-center space-x-2">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {message && (
          <div className="p-3.5 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-xl text-xs flex items-center space-x-2">
            <CheckCircle className="w-4 h-4 shrink-0" />
            <span>{message}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Email Address</label>
            <input 
              type="email" 
              required
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="Enter email address" 
              className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl px-4 py-3 text-sm focus:outline-none transition-colors"
            />
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white font-medium rounded-xl transition-all shadow-md shadow-blue-500/20"
          >
            {loading ? 'Sending link...' : 'Send Reset Link'}
          </button>
        </form>

        <div className="text-center text-xs text-slate-400">
          <button onClick={() => setPage('login')} className="text-blue-500 hover:underline">Back to Login</button>
        </div>
      </div>
    </div>
  );
}

// ----------------------------------------------------
// 3b. RESET PASSWORD PAGE
// ----------------------------------------------------
function ResetPasswordPage({ setPage, resetToken }) {
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (password !== confirmPassword) {
      setError('Passwords do not match!');
      return;
    }

    setLoading(true);
    setError('');
    setMessage('');

    try {
      const res = await axios.post(`${API_BASE}/auth/reset-password`, { 
        token: resetToken, 
        password 
      });
      if (res.data.success) {
        setMessage(res.data.message || 'Password reset successfully!');
        setTimeout(() => {
          // Clear query params in the URL bar
          window.history.replaceState({}, document.title, window.location.pathname);
          setPage('login');
        }, 3000);
      }
    } catch (err) {
      if (err.response) {
        setError(err.response.data?.message || 'Failed to reset password!');
      } else {
        setError('Network error! Please try again later.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md w-full mx-auto py-12">
      <div className="glass-panel border border-slate-800 rounded-2xl p-8 shadow-2xl space-y-6">
        <div className="text-center space-y-2">
          <h2 className="text-2xl font-bold">Reset Password</h2>
          <p className="text-xs text-slate-400">Specify your new secure credentials for your account</p>
        </div>

        {error && (
          <div className="p-3.5 bg-rose-500/10 border border-rose-500/20 text-rose-400 rounded-xl text-xs flex items-center space-x-2">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {message && (
          <div className="p-3.5 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-xl text-xs flex items-center space-x-2">
            <CheckCircle className="w-4 h-4 shrink-0" />
            <span>{message} Redirecting to login...</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">New Password</label>
            <div className="relative">
              <input 
                type={showPassword ? "text" : "password"} 
                required
                minLength={6}
                value={password}
                onChange={e => setPassword(e.target.value)}
                placeholder="••••••••" 
                className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl pl-4 pr-12 py-3 text-sm focus:outline-none transition-colors"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors focus:outline-none cursor-pointer"
              >
                {showPassword ? (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.822 7.822L21 21m-3.228-3.228l-3.65-3.65M12 9.75a2.25 2.25 0 103.5 3.5m-3.5-3.5L12.75 12"></path>
                  </svg>
                ) : (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"></path>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                  </svg>
                )}
              </button>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Confirm Password</label>
            <div className="relative">
              <input 
                type={showConfirmPassword ? "text" : "password"} 
                required
                value={confirmPassword}
                onChange={e => setConfirmPassword(e.target.value)}
                placeholder="••••••••" 
                className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl pl-4 pr-12 py-3 text-sm focus:outline-none transition-colors"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors focus:outline-none cursor-pointer"
              >
                {showConfirmPassword ? (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.822 7.822L21 21m-3.228-3.228l-3.65-3.65M12 9.75a2.25 2.25 0 103.5 3.5m-3.5-3.5L12.75 12"></path>
                  </svg>
                ) : (
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"></path>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                  </svg>
                )}
              </button>
            </div>
          </div>

          <button 
            type="submit" 
            disabled={loading}
            className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white font-medium rounded-xl transition-all shadow-md shadow-blue-500/20"
          >
            {loading ? 'Resetting password...' : 'Update Password'}
          </button>
        </form>
      </div>
    </div>
  );
}

// ----------------------------------------------------
// 4. USER DASHBOARD
// ----------------------------------------------------
function UserDashboard({ setPage, activeToken, tokenPosition, setActiveToken, activeTokens = [], tokenPositions = {} }) {
  const { user } = useAuth();
  const [history, setHistory] = useState([]);
  const [refreshing, setRefreshing] = useState(false);

  const fetchHistory = async () => {
    setRefreshing(true);
    try {
      const res = await axios.get(`${API_BASE}/token/user/${user.userId}`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        setHistory(res.data.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, []);

  const handleCancelToken = async (tokenId) => {
    if (!window.confirm("Are you sure you want to cancel this queue token?")) return;
    try {
      const res = await axios.post(`${API_BASE}/token/${tokenId}/cancel`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        setActiveToken(null);
        fetchHistory();
      }
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="space-y-8">
      {/* Welcome header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-3xl font-extrabold tracking-tight">Active Operations Portal</h2>
          <p className="text-sm text-slate-400">Browse different departments, check active queue statuses, and book visits.</p>
        </div>
        <button 
          onClick={fetchHistory}
          disabled={refreshing}
          className="flex items-center space-x-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold rounded-xl text-slate-300 transition-colors"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin' : ''}`} />
          <span>Sync Status</span>
        </button>
      </div>

      {/* Active Live Token Position Panel */}
      {activeTokens && activeTokens.length > 0 ? (
        <div className="space-y-4">
          <h3 className="text-xl font-bold flex items-center gap-2">
            <Clock className="w-5 h-5 text-blue-500" />
            Active Live Tickets ({activeTokens.length})
          </h3>
          <div className="grid grid-cols-1 gap-6">
            {activeTokens.map(tok => {
              const pos = tokenPositions[tok.id] || { servingToken: 'None', peopleAhead: '-', waitingTime: '-', priority: tok.priority };
              return (
                <div key={tok.id} className="rounded-2xl bg-gradient-to-r from-blue-950/60 to-indigo-950/40 border border-blue-900/60 shadow-xl p-6 md:p-8 flex flex-col md:flex-row items-center justify-between gap-8 animate-pulse-subtle">
                  <div className="space-y-4 text-center md:text-left">
                    <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-blue-500/20 text-xs font-semibold text-blue-400">
                      <Clock className="w-3.5 h-3.5" />
                      <span>Currently Serving: <strong className="text-white">{pos.servingToken}</strong></span>
                    </div>
                    <div>
                      <h3 className="text-2xl font-bold tracking-tight">Live Tracker Position</h3>
                      <p className="text-sm text-slate-400 mt-1 capitalize">
                        Sector: {tok.sectorType.toLowerCase()} | Service: {tok.serviceName}
                      </p>
                      {tok.hospitalName && (
                        <p className="text-sm text-slate-300 mt-2 flex items-center gap-1.5 justify-center md:justify-start">
                          <Building className="w-3.5 h-3.5 text-blue-400 shrink-0" />
                          <span><strong>Facility:</strong> {tok.hospitalName}</span>
                        </p>
                      )}
                      {tok.doctorName && (
                        <p className="text-sm text-slate-300 mt-1 flex items-center gap-1.5 justify-center md:justify-start">
                          <UserIcon className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                          <span><strong>Specialist:</strong> {tok.doctorName}</span>
                        </p>
                      )}
                    </div>
                    <div className="flex flex-wrap gap-4 justify-center md:justify-start pt-2">
                      <div className="px-4 py-2 bg-slate-900/80 rounded-xl border border-slate-800 text-center min-w-[100px]">
                        <div className="text-slate-500 text-[10px] uppercase font-bold">People Ahead</div>
                        <div className="text-2xl font-bold text-blue-400">{pos.peopleAhead}</div>
                      </div>
                      <div className="px-4 py-2 bg-slate-900/80 rounded-xl border border-slate-800 text-center min-w-[100px]">
                        <div className="text-slate-500 text-[10px] uppercase font-bold">Est. Wait Time</div>
                        <div className="text-2xl font-bold text-amber-400">{pos.waitingTime} mins</div>
                      </div>
                      <div className="px-4 py-2 bg-slate-900/80 rounded-xl border border-slate-800 text-center min-w-[100px]">
                        <div className="text-slate-500 text-[10px] uppercase font-bold">Priority Status</div>
                        <div className="text-2xl font-bold text-violet-400">{tok.priority}</div>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-col items-center justify-center space-y-4 shrink-0 bg-slate-900/50 p-6 rounded-2xl border border-slate-800/80 min-w-[200px]">
                    <span className="text-xs text-slate-500 uppercase font-bold tracking-wider">Your Ticket Number</span>
                    <span className="text-5xl font-extrabold bg-gradient-to-r from-blue-500 to-indigo-400 bg-clip-text text-transparent">
                      {tok.tokenNumber}
                    </span>
                    <span className="text-xs font-semibold py-1 px-3 bg-blue-500/10 text-blue-400 rounded-full capitalize">
                      {tok.status.toLowerCase()}
                    </span>
                    <button 
                      onClick={() => handleCancelToken(tok.id)}
                      className="mt-2 text-xs font-semibold text-rose-400 hover:underline"
                    >
                      Cancel Token
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        <div className="p-8 rounded-2xl bg-slate-900/20 border border-slate-800 text-center space-y-2">
          <Clock className="w-10 h-10 text-slate-600 mx-auto" />
          <h4 className="font-semibold text-slate-300">No Active Ticket</h4>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">You don't have any active tokens waiting in a queue. Generate a new token below by selecting a sector.</p>
        </div>
      )}

      {/* Select Sector Grid */}
      <div className="space-y-4">
        <h3 className="text-xl font-bold">Select Active Sector</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div 
            onClick={() => setPage('hospital')}
            className="glass-card rounded-2xl p-6 cursor-pointer relative overflow-hidden"
          >
            <div className="w-12 h-12 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center mb-4">
              <Activity className="w-6 h-6 text-rose-400" />
            </div>
            <h4 className="text-lg font-bold">Hospital Operations</h4>
            <p className="text-slate-400 text-xs mt-2 leading-relaxed">Book clinical specialist visits, access emergency override queues.</p>
            <ChevronRight className="absolute bottom-6 right-6 w-5 h-5 text-slate-600 hover:text-white" />
          </div>

          <div 
            onClick={() => setPage('bank')}
            className="glass-card rounded-2xl p-6 cursor-pointer relative overflow-hidden"
          >
            <div className="w-12 h-12 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center mb-4">
              <Building className="w-6 h-6 text-blue-400" />
            </div>
            <h4 className="text-lg font-bold">Banking Desks</h4>
            <p className="text-slate-400 text-xs mt-2 leading-relaxed">Cash registers, loan consultations, KYC audits, account openings.</p>
            <ChevronRight className="absolute bottom-6 right-6 w-5 h-5 text-slate-600 hover:text-white" />
          </div>

          <div 
            onClick={() => setPage('college')}
            className="glass-card rounded-2xl p-6 cursor-pointer relative overflow-hidden"
          >
            <div className="w-12 h-12 rounded-xl bg-violet-500/10 border border-violet-500/20 flex items-center justify-center mb-4">
              <BookOpen className="w-6 h-6 text-violet-400" />
            </div>
            <h4 className="text-lg font-bold">College Offices</h4>
            <p className="text-slate-400 text-xs mt-2 leading-relaxed">Student registrations, ID cards, fees payment, bonafide approvals.</p>
            <ChevronRight className="absolute bottom-6 right-6 w-5 h-5 text-slate-600 hover:text-white" />
          </div>
        </div>
      </div>

      {/* Booking History Table */}
      <div className="space-y-4">
        <h3 className="text-xl font-bold">Token & Booking Logs</h3>
        <div className="glass-panel border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-900/60 border-b border-slate-800 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  <th className="px-6 py-4">Token Number</th>
                  <th className="px-6 py-4">Department / Service</th>
                  <th className="px-6 py-4">Priority</th>
                  <th className="px-6 py-4">Issued Timestamp</th>
                  <th className="px-6 py-4">Current Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-xs">
                {history.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="text-center py-8 text-slate-500">No token logs available</td>
                  </tr>
                ) : (
                  history.map(item => (
                    <tr key={item.id} className="hover:bg-slate-800/20 transition-colors">
                      <td className="px-6 py-4 font-bold text-blue-400">{item.tokenNumber}</td>
                      <td className="px-6 py-4">
                        <div className="font-semibold text-slate-200 capitalize">{item.sectorType.toLowerCase()}</div>
                        <div className="text-[10px] text-slate-400">{item.serviceName}</div>
                      </td>
                      <td className="px-6 py-4">
                        <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-semibold border ${item.priority === 'EMERGENCY' ? 'bg-rose-500/10 border-rose-500/20 text-rose-400' : 'bg-slate-800 border-slate-700 text-slate-400'}`}>
                          {item.priority}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-slate-400">{new Date(item.createdAt).toLocaleString()}</td>
                      <td className="px-6 py-4">
                        <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-semibold border ${
                          item.status === 'COMPLETED' ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' :
                          item.status === 'SERVING' ? 'bg-amber-500/10 border-amber-500/20 text-amber-400 animate-pulse' :
                          item.status === 'SKIPPED' ? 'bg-slate-800 border-slate-700 text-slate-500' :
                          item.status === 'CANCELLED' ? 'bg-rose-500/10 border-rose-500/20 text-rose-400' :
                          'bg-blue-500/10 border-blue-500/20 text-blue-400'
                        }`}>
                          {item.status}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

function HospitalModule({ setPage }) {
  const { user } = useAuth();
  const [hospitals, setHospitals] = useState([]);
  const [branches, setBranches] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [slots, setSlots] = useState([]);
  
  const [selectedHosp, setSelectedHosp] = useState(null);
  const [selectedBranch, setSelectedBranch] = useState(null);
  const [selectedDoc, setSelectedDoc] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [priority, setPriority] = useState('REGULAR');
  const [service, setService] = useState('General Consultation');
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);

  // Checkout states
  const [showPayment, setShowPayment] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [paying, setPaying] = useState(false);
  const [paymentSuccessData, setPaymentSuccessData] = useState(null);

  useEffect(() => {
    // Fetch all hospitals
    axios.get(`${API_BASE}/hospital/list`, {
      headers: { Authorization: `Bearer ${user.token}` }
    }).then(res => {
      if (res.data.success) setHospitals(res.data.data);
    }).catch(err => console.error(err));
  }, []);

  const getConsultationFee = (specialty) => {
    if (!specialty) return 300;
    const spec = specialty.toLowerCase();
    if (spec.includes('cardio')) return 1000;
    if (spec.includes('derm')) return 700;
    if (spec.includes('ent')) return 500;
    if (spec.includes('neuro')) return 800;
    if (spec.includes('ortho')) return 600;
    return 300;
  };

  const handleSelectHosp = async (hosp) => {
    setSelectedHosp(hosp);
    setSelectedBranch(null);
    setSelectedDoc(null);
    setSelectedSlot(null);
    setBranches([]);
    setDoctors([]);
    setSlots([]);

    try {
      const res = await axios.get(`${API_BASE}/hospital/${hosp.id}/branches`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) setBranches(res.data.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleSelectBranch = async (br) => {
    setSelectedBranch(br);
    setSelectedDoc(null);
    setSelectedSlot(null);
    setDoctors([]);
    setSlots([]);

    try {
      const res = await axios.get(`${API_BASE}/hospital/branch/${br.id}/doctors`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) setDoctors(res.data.data);
    } catch (err) {
      console.error(err);
    }
  };

  const getDiseaseOptions = (specialty) => {
    if (!specialty) return ['General Consultation'];
    const spec = specialty.toLowerCase();
    if (spec.includes('cardio')) {
      return ['Chest Pain Checkup', 'Heart Rate Screening', 'Cardiology Consult', 'Blood Pressure Check'];
    }
    if (spec.includes('neuro')) {
      return ['Migraine & Headaches', 'Nerve Pain Screening', 'Neurology Followup', 'Neuromuscular Evaluation'];
    }
    if (spec.includes('ortho')) {
      return ['Joint & Bone Pain', 'Fracture follow-up', 'Spine & Back Consult', 'Arthritis checkup'];
    }
    switch (specialty) {
      case 'Dermatologist':
      case 'Dermatology':
        return ['Skin Allergy', 'Acne treatment', 'Hair Loss', 'Eczema Checkup'];
      case 'Pediatrician':
      case 'Pediatrics':
        return ['Fever & Cold (Child)', 'Childhood Vaccination', 'Growth Consultation', 'Pediatric Cough'];
      case 'ENT Specialist':
      case 'ENT':
        return ['Ear Infection', 'Sinusitis', 'Tonsils Consultation', 'Hearing Checkup'];
      default:
        return ['Fever', 'Cold & Cough', 'General Health Query', 'Diabetes Checkup'];
    }
  };

  const fetchDoctorSlots = async (docId, dateStr) => {
    try {
      const res = await axios.get(`${API_BASE}/hospital/doctor/${docId}/slots?date=${dateStr}`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) setSlots(res.data.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleSelectDoc = async (doc) => {
    setSelectedDoc(doc);
    setSelectedSlot(null);
    setSlots([]);
    setService(getDiseaseOptions(doc.specialty)[0]);
    await fetchDoctorSlots(doc.id, selectedDate);
  };

  const handleDateChange = async (newDate) => {
    setSelectedDate(newDate);
    setSelectedSlot(null);
    setSlots([]);
    if (selectedDoc) {
      await fetchDoctorSlots(selectedDoc.id, newDate);
    }
  };

  const handlePaymentAndBook = async () => {
    setPaying(true);
    try {
      const fee = selectedDoc.consultationFee || getConsultationFee(selectedDoc.specialization || selectedDoc.specialty);
      const encodedService = encodeURIComponent(service);
      // 1. Initiate Payment
      const payRes = await axios.post(`${API_BASE}/payment/initiate?userId=${user.userId}&sectorType=HOSPITAL&amount=${fee}&paymentMethod=${paymentMethod}`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      
      if (payRes.data.success) {
        const payment = payRes.data.data;
        // 2. Book appointment
        const bookRes = await axios.post(`${API_BASE}/hospital/book?userId=${user.userId}&slotId=${selectedSlot.id}&doctorId=${selectedDoc.id}&serviceName=${encodedService}`, {}, {
          headers: { Authorization: `Bearer ${user.token}` }
        });
        
        if (bookRes.data.success) {
          const appt = bookRes.data.data;
          // 3. Generate Token — referenceId = doctorId (backend resolves to branchId automatically)
          const tokRes = await axios.post(`${API_BASE}/token/generate?userId=${user.userId}&sectorType=HOSPITAL&referenceId=${selectedDoc.id}&serviceName=${encodedService}&appointmentId=${appt.id}&priority=${priority}`, {}, {
            headers: { Authorization: `Bearer ${user.token}` }
          });
          
          if (tokRes.data.success) {
            setPaymentSuccessData({
              tokenNumber: tokRes.data.data.tokenNumber,
              transactionId: payment.transactionId,
              paymentId: payment.id
            });
          } else {
            alert('Token generation failed: ' + (tokRes.data.message || 'Unknown error'));
          }
        } else {
          alert('Appointment booking failed: ' + (bookRes.data.message || 'Slot may be full'));
        }
      }
    } catch (err) {
      console.error('Payment/Booking error:', err);
      alert(err.response?.data?.message || err.message || "Booking or Payment failed!");
    } finally {
      setPaying(false);
    }
  };

  return (
    <div className="space-y-8">
      <div className="pb-4 border-b border-slate-900">
        <h2 className="text-3xl font-extrabold">Hospital Booking Module</h2>
        <p className="text-xs text-slate-400 mt-1">Select specialized centers and book tokens in live queues.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Hospitals Selection */}
        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">1. Select Hospital</span>
          <div className="space-y-2">
            {hospitals.map(h => (
              <div 
                key={h.id} 
                onClick={() => handleSelectHosp(h)}
                className={`p-4 rounded-xl border cursor-pointer transition-colors ${selectedHosp?.id === h.id ? 'bg-blue-600/10 border-blue-500 text-blue-400' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/40'}`}
              >
                <div className="font-bold text-sm text-slate-200">{h.name}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Branches Selection */}
        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">2. Select Branch</span>
          <div className="space-y-2">
            {selectedHosp ? (
              branches.length === 0 ? (
                <div className="text-xs text-slate-500">No branches found</div>
              ) : (
                branches.map(b => (
                  <div 
                    key={b.id} 
                    onClick={() => handleSelectBranch(b)}
                    className={`p-4 rounded-xl border cursor-pointer transition-colors ${selectedBranch?.id === b.id ? 'bg-blue-600/10 border-blue-500 text-blue-400' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/40'}`}
                  >
                    <div className="font-bold text-sm text-slate-200">{b.name}</div>
                    <div className="text-[10px] text-slate-400 mt-1">{b.location}</div>
                  </div>
                ))
              )
            ) : (
              <div className="text-xs text-slate-500">Please select hospital first</div>
            )}
          </div>
        </div>

        {/* Doctors Selection */}
        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">3. Specialist Doctor</span>
          <div className="space-y-2">
            {selectedBranch ? (
              doctors.length === 0 ? (
                <div className="text-xs text-slate-500">No doctors registered</div>
              ) : (
                doctors.map(d => (
                  <div 
                    key={d.id} 
                    onClick={() => {
                      if (d.availabilityStatus !== false) {
                        handleSelectDoc(d);
                      } else {
                        alert(`${d.name} is currently unavailable! Please select another specialist.`);
                      }
                    }}
                    className={`p-4 rounded-xl border transition-all hover:scale-[1.01] ${d.availabilityStatus === false ? 'opacity-50 cursor-not-allowed bg-slate-950 border-slate-900' : selectedDoc?.id === d.id ? 'bg-blue-600/10 border-blue-500 text-blue-400 cursor-pointer' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/40 cursor-pointer'}`}
                  >
                    <div className="flex justify-between items-start">
                      <div>
                        <div className="font-bold text-sm text-slate-200">{d.name}</div>
                        <div className="text-[10px] text-slate-400 mt-1">{d.specialization || d.specialty}</div>
                      </div>
                      {d.availabilityStatus !== false ? (
                        <span className="px-2 py-0.5 rounded-md bg-emerald-950/40 border border-emerald-900/30 text-[10px] font-semibold text-emerald-400 shrink-0 select-none">
                          Available
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 rounded-md bg-rose-950/40 border border-rose-900/30 text-[10px] font-semibold text-rose-400 shrink-0 select-none">
                          Unavailable
                        </span>
                      )}
                    </div>
                    <div className="inline-flex items-center space-x-1.5 px-2 py-0.5 mt-3 rounded-md bg-blue-950/40 border border-blue-900/30 text-[10px] font-bold text-blue-400">
                      <span>Fee: ₹{d.consultationFee || getConsultationFee(d.specialization || d.specialty)}</span>
                    </div>
                  </div>
                ))
              )
            ) : (
              <div className="text-xs text-slate-500">Please select branch first</div>
            )}
          </div>
        </div>

        {/* Slot Selection & Form Booking */}
        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">4. Booking Panel</span>
          <div className="glass-panel border border-slate-800 rounded-xl p-4 space-y-4">
            {selectedDoc ? (
              <>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Select Date</label>
                  <input 
                    type="date" 
                    value={selectedDate}
                    min={new Date().toISOString().split('T')[0]}
                    onChange={e => handleDateChange(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-lg px-3 py-2 text-xs focus:outline-none transition-colors text-slate-200"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Select Slot</label>
                  <div className="grid grid-cols-2 gap-2">
                    {slots.length === 0 ? (
                      <div className="text-xs text-slate-500 col-span-2">No slots available today</div>
                    ) : (
                      slots.map(s => (
                        <button
                          key={s.id}
                          disabled={s.bookedTokens >= s.maxTokens}
                          onClick={() => setSelectedSlot(s)}
                          className={`p-2 rounded-lg border text-left text-[11px] font-medium transition-colors ${s.bookedTokens >= s.maxTokens ? 'bg-slate-950 border-slate-900 text-slate-600 cursor-not-allowed' : selectedSlot?.id === s.id ? 'bg-blue-600 border-blue-500 text-white' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/60'}`}
                        >
                          <div>{s.startTime.substring(0, 5)} - {s.endTime.substring(0, 5)}</div>
                          <div className="text-[9px] mt-0.5 opacity-80">{s.maxTokens - s.bookedTokens} left</div>
                        </button>
                      ))
                    )}
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Consultation Disease / Symptom</label>
                  <select 
                    value={service}
                    onChange={e => setService(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-lg px-3 py-2 text-xs focus:outline-none transition-colors"
                  >
                    {getDiseaseOptions(selectedDoc.specialty).map(opt => (
                      <option key={opt} value={opt}>{opt}</option>
                    ))}
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Priority Level</label>
                  <select 
                    value={priority}
                    onChange={e => setPriority(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-lg px-3 py-2 text-xs focus:outline-none transition-colors"
                  >
                    <option value="REGULAR">Regular (FIFO queue)</option>
                    <option value="EMERGENCY">Emergency (Jump line queue)</option>
                  </select>
                </div>

                <div className="p-3 bg-slate-950/60 rounded-xl border border-slate-900 flex justify-between items-center text-xs">
                  <span className="font-semibold text-slate-400">Consultation Fee:</span>
                  <span className="font-extrabold text-blue-400">₹{selectedDoc.consultationFee || getConsultationFee(selectedDoc.specialization || selectedDoc.specialty)}</span>
                </div>

                <button 
                  onClick={() => setShowPayment(true)}
                  disabled={!selectedSlot}
                  className="w-full py-2.5 bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold rounded-lg transition-colors shadow-md shadow-blue-500/20 disabled:opacity-40 cursor-pointer"
                >
                  Generate Token
                </button>
              </>
            ) : (
              <div className="text-xs text-slate-500 py-4 text-center">Please select doctor first</div>
            )}
          </div>
        </div>
      </div>

      {/* Checkout Modal */}
      {showPayment && !paymentSuccessData && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-in fade-in duration-200">
          <div className="glass-panel border border-slate-800 rounded-2xl p-6 max-w-md w-full space-y-6 text-xs">
            <div className="text-center space-y-1">
              <h3 className="text-lg font-bold text-slate-100">Consultation Fee Checkout</h3>
              <p className="text-slate-400">Complete your payment to confirm slot and generate ticket</p>
            </div>
            
            <div className="p-4 bg-slate-950/60 rounded-xl border border-slate-900 space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-500">Doctor:</span>
                <span className="font-semibold text-slate-200">{selectedDoc?.name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Specialty:</span>
                <span className="font-semibold text-slate-400">{selectedDoc?.specialization || selectedDoc?.specialty}</span>
              </div>
              <div className="flex justify-between border-t border-slate-900 pt-2 text-sm">
                <span className="font-bold text-slate-300">Total Consultation Fee:</span>
                <span className="font-extrabold text-blue-400">₹{selectedDoc?.consultationFee || getConsultationFee(selectedDoc?.specialization || selectedDoc?.specialty)}</span>
              </div>
            </div>

            <div className="space-y-2">
              <label className="font-bold text-slate-400">Select Payment Method</label>
              <select
                value={paymentMethod}
                onChange={e => setPaymentMethod(e.target.value)}
                className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-slate-200 focus:outline-none"
              >
                <option value="UPI">UPI Payment</option>
                <option value="DEBIT_CARD">Debit Card</option>
                <option value="CREDIT_CARD">Credit Card</option>
                <option value="NET_BANKING">Net Banking</option>
                <option value="CASH_COUNTER">Cash Counter Option</option>
              </select>
            </div>

            <div className="flex items-center space-x-3 pt-2">
              <button
                onClick={handlePaymentAndBook}
                disabled={paying}
                className="flex-1 py-3 bg-blue-600 hover:bg-blue-500 disabled:bg-slate-800 disabled:text-slate-500 text-white font-semibold rounded-xl flex items-center justify-center gap-2 shadow-md shadow-blue-500/10 cursor-pointer"
              >
                {paying ? 'Processing Transaction...' : 'Pay & Generate Token'}
              </button>
              <button
                onClick={() => setShowPayment(false)}
                disabled={paying}
                className="px-5 py-3 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 font-semibold rounded-xl cursor-pointer"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Success Receipt Modal */}
      {paymentSuccessData && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 backdrop-blur-sm p-4 animate-in fade-in duration-200">
          <div className="glass-panel border border-slate-800 rounded-2xl p-8 max-w-md w-full space-y-6 text-center text-xs">
            <div className="w-16 h-16 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center mx-auto">
              <CheckCircle className="w-8 h-8 text-emerald-400" />
            </div>

            <div className="space-y-1">
              <h3 className="text-xl font-bold text-slate-100">Booking Confirmed!</h3>
              <p className="text-slate-400">Payment completed & slot booked successfully</p>
            </div>

            <div className="p-6 bg-slate-950/60 rounded-2xl border border-slate-900 space-y-3">
              <div>
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Your Ticket Number</span>
                <div className="text-4xl font-extrabold text-blue-400 mt-1">{paymentSuccessData.tokenNumber}</div>
              </div>
              <div className="border-t border-slate-900 pt-3 flex justify-between text-[11px]">
                <span className="text-slate-500">Transaction ID:</span>
                <span className="font-mono font-semibold text-slate-300">{paymentSuccessData.transactionId}</span>
              </div>
            </div>

            <div className="space-y-3">
              <button
                onClick={() => window.open(`${API_BASE}/payment/receipt/${paymentSuccessData.paymentId}`)}
                className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-xl shadow-md shadow-blue-500/10 flex items-center justify-center gap-2 cursor-pointer"
              >
                <FileText className="w-4 h-4" />
                Download PDF Receipt
              </button>
              <button
                onClick={() => {
                  setPaymentSuccessData(null);
                  setShowPayment(false);
                  setPage('dashboard');
                }}
                className="w-full py-3 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 font-semibold rounded-xl cursor-pointer"
              >
                Go to Dashboard
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ----------------------------------------------------
// 6. BANK MODULE
// ----------------------------------------------------
function BankModule({ setPage }) {
  const { user } = useAuth();
  const [banks, setBanks] = useState([]);
  const [branches, setBranches] = useState([]);
  const [counters, setCounters] = useState([]);
  const [slots, setSlots] = useState([]);
  
  const [selectedBank, setSelectedBank] = useState(null);
  const [selectedBranch, setSelectedBranch] = useState(null);
  const [selectedCounter, setSelectedCounter] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [service, setService] = useState('Cash Deposit');
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);

  useEffect(() => {
    axios.get(`${API_BASE}/bank/list`, {
      headers: { Authorization: `Bearer ${user.token}` }
    }).then(res => {
      if (res.data.success) setBanks(res.data.data);
    }).catch(err => console.error(err));
  }, []);

  const handleSelectBank = async (bank) => {
    setSelectedBank(bank);
    setSelectedBranch(null);
    setSelectedCounter(null);
    setSelectedSlot(null);
    setBranches([]);
    setCounters([]);
    setSlots([]);

    try {
      const res = await axios.get(`${API_BASE}/bank/${bank.id}/branches`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) setBranches(res.data.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleSelectBranch = async (br) => {
    setSelectedBranch(br);
    setSelectedCounter(null);
    setSelectedSlot(null);
    setCounters([]);
    setSlots([]);

    try {
      const res = await axios.get(`${API_BASE}/bank/branch/${br.id}/counters`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) setCounters(res.data.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchCounterSlots = async (cntId, dateStr) => {
    try {
      const res = await axios.get(`${API_BASE}/bank/counter/${cntId}/slots?date=${dateStr}`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) setSlots(res.data.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleSelectCounter = async (cnt) => {
    setSelectedCounter(cnt);
    setSelectedSlot(null);
    setSlots([]);
    await fetchCounterSlots(cnt.id, selectedDate);
  };

  const handleDateChange = async (newDate) => {
    setSelectedDate(newDate);
    setSelectedSlot(null);
    setSlots([]);
    if (selectedCounter) {
      await fetchCounterSlots(selectedCounter.id, newDate);
    }
  };

  const handleBook = async () => {
    if (!selectedSlot || !selectedBranch || !selectedCounter) return;
    try {
      // 1. Book appointment
      const bookRes = await axios.post(`${API_BASE}/bank/book?userId=${user.userId}&slotId=${selectedSlot.id}&branchId=${selectedBranch.id}&serviceName=${service}`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (bookRes.data.success) {
        const appt = bookRes.data.data;
        // 2. Generate token for that appointment
        const tokRes = await axios.post(`${API_BASE}/token/generate?userId=${user.userId}&sectorType=BANK&referenceId=${selectedBranch.id}&serviceName=${service}&appointmentId=${appt.id}&priority=REGULAR`, {}, {
          headers: { Authorization: `Bearer ${user.token}` }
        });
        if (tokRes.data.success) {
          alert("Bank Token generated successfully!");
          setPage('dashboard');
        }
      }
    } catch (err) {
      alert(err.response?.data?.message || "Booking failed!");
    }
  };

  return (
    <div className="space-y-8">
      <div className="pb-4 border-b border-slate-900">
        <h2 className="text-3xl font-extrabold">Banking Counter Module</h2>
        <p className="text-xs text-slate-400 mt-1">Select branches, define service, and register counter tickets.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">1. Select Bank</span>
          <div className="space-y-2">
            {banks.map(b => (
              <div 
                key={b.id} 
                onClick={() => handleSelectBank(b)}
                className={`p-4 rounded-xl border cursor-pointer transition-colors ${selectedBank?.id === b.id ? 'bg-blue-600/10 border-blue-500 text-blue-400' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/40'}`}
              >
                <div className="font-bold text-sm text-slate-200">{b.name}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">2. Select Branch</span>
          <div className="space-y-2">
            {selectedBank ? (
              branches.length === 0 ? (
                <div className="text-xs text-slate-500">No branches found</div>
              ) : (
                branches.map(b => (
                  <div 
                    key={b.id} 
                    onClick={() => handleSelectBranch(b)}
                    className={`p-4 rounded-xl border cursor-pointer transition-colors ${selectedBranch?.id === b.id ? 'bg-blue-600/10 border-blue-500 text-blue-400' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/40'}`}
                  >
                    <div className="font-bold text-sm text-slate-200">{b.name}</div>
                    <div className="text-[10px] text-slate-400 mt-1">{b.location}</div>
                  </div>
                ))
              )
            ) : (
              <div className="text-xs text-slate-500">Please select bank first</div>
            )}
          </div>
        </div>

        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">3. Counter Details</span>
          <div className="space-y-2">
            {selectedBranch ? (
              counters.length === 0 ? (
                <div className="text-xs text-slate-500">No counters active</div>
              ) : (
                counters.map(c => (
                  <div 
                    key={c.id} 
                    onClick={() => handleSelectCounter(c)}
                    className={`p-4 rounded-xl border cursor-pointer transition-colors ${selectedCounter?.id === c.id ? 'bg-blue-600/10 border-blue-500 text-blue-400' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/40'}`}
                  >
                    <div className="font-bold text-sm text-slate-200">{c.name}</div>
                    <div className="text-[10px] text-slate-400 mt-1">Status: {c.status}</div>
                  </div>
                ))
              )
            ) : (
              <div className="text-xs text-slate-500">Please select branch first</div>
            )}
          </div>
        </div>

        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">4. Booking Panel</span>
          <div className="glass-panel border border-slate-800 rounded-xl p-4 space-y-4">
            {selectedCounter ? (
              <>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Select Date</label>
                  <input 
                    type="date" 
                    value={selectedDate}
                    min={new Date().toISOString().split('T')[0]}
                    onChange={e => handleDateChange(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-lg px-3 py-2 text-xs focus:outline-none transition-colors text-slate-200"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Select Slot</label>
                  <div className="grid grid-cols-2 gap-2">
                    {slots.length === 0 ? (
                      <div className="text-xs text-slate-500 col-span-2">No slots available today</div>
                    ) : (
                      slots.map(s => (
                        <button
                          key={s.id}
                          disabled={s.bookedTokens >= s.maxTokens}
                          onClick={() => setSelectedSlot(s)}
                          className={`p-2 rounded-lg border text-left text-[11px] font-medium transition-colors ${s.bookedTokens >= s.maxTokens ? 'bg-slate-950 border-slate-900 text-slate-600 cursor-not-allowed' : selectedSlot?.id === s.id ? 'bg-blue-600 border-blue-500 text-white' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/60'}`}
                        >
                          <div>{s.startTime.substring(0, 5)} - {s.endTime.substring(0, 5)}</div>
                          <div className="text-[9px] mt-0.5 opacity-80">{s.maxTokens - s.bookedTokens} left</div>
                        </button>
                      ))
                    )}
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Banking Service</label>
                  <select 
                    value={service}
                    onChange={e => setService(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-lg px-3 py-2 text-xs focus:outline-none transition-colors"
                  >
                    <option value="Cash Deposit">Cash Deposit</option>
                    <option value="Withdrawal">Withdrawal</option>
                    <option value="KYC Audit">KYC Audit</option>
                    <option value="Loan Consultation">Loan Consultation</option>
                    <option value="Account Opening">Account Opening</option>
                    <option value="Passbook Update">Passbook Update</option>
                  </select>
                </div>

                <button 
                  onClick={handleBook}
                  disabled={!selectedSlot}
                  className="w-full py-2.5 bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold rounded-lg transition-colors shadow-md shadow-blue-500/20 disabled:opacity-40"
                >
                  Generate Token
                </button>
              </>
            ) : (
              <div className="text-xs text-slate-500 py-4 text-center">Please select counter first</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// ----------------------------------------------------
// 7. COLLEGE MODULE
// ----------------------------------------------------
function CollegeModule({ setPage }) {
  const { user } = useAuth();
  const [colleges, setColleges] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [counters, setCounters] = useState([]);
  const [slots, setSlots] = useState([]);
  
  const [selectedCol, setSelectedCol] = useState(null);
  const [selectedDept, setSelectedDept] = useState(null);
  const [selectedCounter, setSelectedCounter] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [service, setService] = useState('Fees Submission');
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [deptStream, setDeptStream] = useState('Computer Science & Engineering');
  const [studentYear, setStudentYear] = useState('1st Year');

  // Checkout states
  const [showPayment, setShowPayment] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [paying, setPaying] = useState(false);
  const [paymentSuccessData, setPaymentSuccessData] = useState(null);

  useEffect(() => {
    axios.get(`${API_BASE}/college/list`, {
      headers: { Authorization: `Bearer ${user.token}` }
    }).then(res => {
      if (res.data.success) setColleges(res.data.data);
    }).catch(err => console.error(err));
  }, []);

  const getCollegeFee = (purpose) => {
    if (!purpose) return 0;
    const purp = purpose.trim();
    if (purp === 'Fees Submission') return 2000;
    return 0;
  };

  const handleSelectCol = async (col) => {
    setSelectedCol(col);
    setSelectedDept(null);
    setSelectedCounter(null);
    setSelectedSlot(null);
    setDepartments([]);
    setCounters([]);
    setSlots([]);

    try {
      const res = await axios.get(`${API_BASE}/college/${col.id}/departments`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) setDepartments(res.data.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleSelectDept = async (dept) => {
    setSelectedDept(dept);
    setSelectedCounter(null);
    setSelectedSlot(null);
    setCounters([]);
    setSlots([]);

    try {
      const res = await axios.get(`${API_BASE}/college/branch/${dept.id}/counters`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) setCounters(res.data.data);
    } catch (err) {
      console.error(err);
    }
  };

  const fetchCollegeSlots = async (cntId, dateStr) => {
    try {
      const res = await axios.get(`${API_BASE}/slots/by-counter/${cntId}?date=${dateStr}`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) setSlots(res.data.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleSelectCounter = async (cnt) => {
    setSelectedCounter(cnt);
    setSelectedSlot(null);
    setSlots([]);
    await fetchCollegeSlots(cnt.id, selectedDate);
  };

  const handleDateChange = async (newDate) => {
    setSelectedDate(newDate);
    setSelectedSlot(null);
    setSlots([]);
    if (selectedCounter) {
      await fetchCollegeSlots(selectedCounter.id, newDate);
    }
  };

  const handleDirectFreeBook = async () => {
    setPaying(true);
    try {
      const finalServiceName = `${service} (${deptStream} - ${studentYear})`;
      const bookRes = await axios.post(`${API_BASE}/college/book?userId=${user.userId}&slotId=${selectedSlot.id}&departmentId=${selectedDept.id}&serviceName=${finalServiceName}`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      
      if (bookRes.data.success) {
        const appt = bookRes.data.data;
        const tokRes = await axios.post(`${API_BASE}/token/generate?userId=${user.userId}&sectorType=COLLEGE&branchId=${selectedDept.id}&counterId=${selectedCounter.id}&serviceName=${finalServiceName}&appointmentId=${appt.id}&priority=REGULAR`, {}, {
          headers: { Authorization: `Bearer ${user.token}` }
        });
        
        if (tokRes.data.success) {
          setPaymentSuccessData({
            tokenNumber: tokRes.data.data.tokenNumber,
            transactionId: 'FREE_OF_COST',
            paymentId: null
          });
        }
      }
    } catch (err) {
      alert(err.response?.data?.message || "Booking failed!");
    } finally {
      setPaying(false);
    }
  };

  const handlePaymentAndBook = async () => {
    setPaying(true);
    try {
      const fee = getCollegeFee(service);
      const finalServiceName = `${service} (${deptStream} - ${studentYear})`;
      // 1. Initiate Payment
      const payRes = await axios.post(`${API_BASE}/payment/initiate?userId=${user.userId}&sectorType=COLLEGE&amount=${fee}&paymentMethod=${paymentMethod}`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      
      if (payRes.data.success) {
        const payment = payRes.data.data;
        // 2. Book appointment
        const bookRes = await axios.post(`${API_BASE}/college/book?userId=${user.userId}&slotId=${selectedSlot.id}&departmentId=${selectedDept.id}&serviceName=${finalServiceName}`, {}, {
          headers: { Authorization: `Bearer ${user.token}` }
        });
        
        if (bookRes.data.success) {
          const appt = bookRes.data.data;
          // 3. Generate Token
          const tokRes = await axios.post(`${API_BASE}/token/generate?userId=${user.userId}&sectorType=COLLEGE&branchId=${selectedDept.id}&counterId=${selectedCounter.id}&serviceName=${finalServiceName}&appointmentId=${appt.id}&priority=REGULAR`, {}, {
            headers: { Authorization: `Bearer ${user.token}` }
          });
          
          if (tokRes.data.success) {
            setPaymentSuccessData({
              tokenNumber: tokRes.data.data.tokenNumber,
              transactionId: payment.transactionId,
              paymentId: payment.id
            });
          }
        }
      }
    } catch (err) {
      alert(err.response?.data?.message || "Booking or Payment failed!");
    } finally {
      setPaying(false);
    }
  };

  return (
    <div className="space-y-8">
      <div className="pb-4 border-b border-slate-900">
        <h2 className="text-3xl font-extrabold">College Office Module</h2>
        <p className="text-xs text-slate-400 mt-1">Select departments, request services, and verify positions.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Colleges Selection */}
        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">1. Select College</span>
          <div className="space-y-2">
            {colleges.map(c => (
              <div 
                key={c.id} 
                onClick={() => handleSelectCol(c)}
                className={`p-4 rounded-xl border cursor-pointer transition-colors ${selectedCol?.id === c.id ? 'bg-blue-600/10 border-blue-500 text-blue-400' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/40'}`}
              >
                <div className="font-bold text-sm text-slate-200">{c.name}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Campuses Selection */}
        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">2. Select Branch/Campus</span>
          <div className="space-y-2">
            {selectedCol ? (
              departments.length === 0 ? (
                <div className="text-xs text-slate-500">No campuses found</div>
              ) : (
                departments.map(d => (
                  <div 
                    key={d.id} 
                    onClick={() => handleSelectDept(d)}
                    className={`p-4 rounded-xl border cursor-pointer transition-colors ${selectedDept?.id === d.id ? 'bg-blue-600/10 border-blue-500 text-blue-400' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/40'}`}
                  >
                    <div className="font-bold text-sm text-slate-200">{d.name}</div>
                    <div className="text-[10px] text-slate-400 mt-1">{d.officeName} | {d.buildingDetails}</div>
                  </div>
                ))
              )
            ) : (
              <div className="text-xs text-slate-500">Please select college first</div>
            )}
          </div>
        </div>

        {/* Counters Selection */}
        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">3. Select Counter</span>
          <div className="space-y-2">
            {selectedDept ? (
              counters.length === 0 ? (
                <div className="text-xs text-slate-500">No counters active</div>
              ) : (
                counters.map(c => (
                  <div 
                    key={c.id} 
                    onClick={() => handleSelectCounter(c)}
                    className={`p-4 rounded-xl border cursor-pointer transition-colors ${selectedCounter?.id === c.id ? 'bg-blue-600/10 border-blue-500 text-blue-400' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/40'}`}
                  >
                    <div className="font-bold text-sm text-slate-200">{c.counterName}</div>
                    <div className="text-[10px] text-slate-400 mt-1">Status: {c.status}</div>
                  </div>
                ))
              )
            ) : (
              <div className="text-xs text-slate-500">Please select branch/campus first</div>
            )}
          </div>
        </div>

        {/* Booking Panel */}
        <div className="space-y-3">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-500">4. Booking Panel</span>
          <div className="glass-panel border border-slate-800 rounded-xl p-4 space-y-4">
            {selectedCounter ? (
              <>
                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Select Date</label>
                  <input 
                    type="date" 
                    value={selectedDate}
                    min={new Date().toISOString().split('T')[0]}
                    onChange={e => handleDateChange(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-lg px-3 py-2 text-xs focus:outline-none transition-colors text-slate-200"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Select Slot</label>
                  <div className="grid grid-cols-2 gap-2">
                    {slots.length === 0 ? (
                      <div className="text-xs text-slate-500 col-span-2">No slots available today</div>
                    ) : (
                      slots.map(s => (
                        <button
                          key={s.id}
                          type="button"
                          disabled={s.bookedTokens >= s.maxTokens}
                          onClick={() => setSelectedSlot(s)}
                          className={`p-2 rounded-lg border text-left text-[11px] font-medium transition-colors ${s.bookedTokens >= s.maxTokens ? 'bg-slate-950 border-slate-900 text-slate-600 cursor-not-allowed' : selectedSlot?.id === s.id ? 'bg-blue-600 border-blue-500 text-white' : 'bg-slate-900 border-slate-800 hover:bg-slate-800/60'}`}
                        >
                          <div>{s.startTime.substring(0, 5)} - {s.endTime.substring(0, 5)}</div>
                          <div className="text-[9px] mt-0.5 opacity-80">{s.maxTokens - s.bookedTokens} left</div>
                        </button>
                      ))
                    )}
                  </div>
                </div>

                 <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Select Department</label>
                  <select
                    value={deptStream}
                    onChange={e => setDeptStream(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-lg px-3 py-2 text-xs focus:outline-none transition-colors text-slate-200"
                  >
                    <option value="Computer Science & Engineering">Computer Science & Engineering</option>
                    <option value="Information Technology">Information Technology</option>
                    <option value="Electronics & Communication">Electronics & Communication</option>
                    <option value="Mechanical Engineering">Mechanical Engineering</option>
                    <option value="Civil Engineering">Civil Engineering</option>
                    <option value="Electrical Engineering">Electrical Engineering</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Select Academic Year</label>
                  <select
                    value={studentYear}
                    onChange={e => setStudentYear(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-lg px-3 py-2 text-xs focus:outline-none transition-colors text-slate-200"
                  >
                    <option value="1st Year">1st Year</option>
                    <option value="2nd Year">2nd Year</option>
                    <option value="3rd Year">3rd Year</option>
                    <option value="4th Year">4th Year</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-bold uppercase text-slate-400">Request Purpose</label>
                  <select 
                    value={service}
                    onChange={e => setService(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-lg px-3 py-2 text-xs focus:outline-none transition-colors text-slate-200"
                  >
                    <option value="Fees Submission">Fees Submission</option>
                    <option value="Admission Cell">Admission Cell</option>
                    <option value="Scholarship Verification">Scholarship Verification</option>
                    <option value="Bonafide Certificate">Bonafide Certificate</option>
                    <option value="ID Card Support">ID Card Support</option>
                    <option value="Exam Form Submission">Exam Form Submission</option>
                  </select>
                </div>

                <div className="p-3 bg-slate-950/60 rounded-xl border border-slate-900 flex justify-between items-center text-xs">
                  <span className="font-semibold text-slate-400">Processing Fee:</span>
                  <span className="font-extrabold text-blue-400">
                    {getCollegeFee(service) > 0 ? `₹${getCollegeFee(service)}` : 'Free of Cost'}
                  </span>
                </div>

                <button 
                  onClick={() => {
                    const fee = getCollegeFee(service);
                    if (fee > 0) {
                      setShowPayment(true);
                    } else {
                      handleDirectFreeBook();
                    }
                  }}
                  disabled={!selectedSlot || paying}
                  className="w-full py-2.5 bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold rounded-lg transition-colors shadow-md shadow-blue-500/20 disabled:opacity-40 cursor-pointer"
                >
                  {paying ? 'Generating...' : 'Generate Token'}
                </button>
              </>
            ) : (
              <div className="text-xs text-slate-500 py-4 text-center">Please select counter first</div>
            )}
          </div>
        </div>
      </div>

      {/* Checkout Modal */}
      {showPayment && !paymentSuccessData && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-in fade-in duration-200">
          <div className="glass-panel border border-slate-800 rounded-2xl p-6 max-w-md w-full space-y-6 text-xs">
            <div className="text-center space-y-1">
              <h3 className="text-lg font-bold text-slate-100">College Portal Checkout</h3>
              <p className="text-slate-400">Complete payment to finalize booking and fetch ticket</p>
            </div>
            
            <div className="p-4 bg-slate-950/60 rounded-xl border border-slate-900 space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-500">College:</span>
                <span className="font-semibold text-slate-200">{selectedCol?.name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Service Counter:</span>
                <span className="font-semibold text-slate-400">{selectedCounter?.counterName}</span>
              </div>
              <div className="flex justify-between border-t border-slate-900 pt-2 text-sm">
                <span className="font-bold text-slate-300">Total Administrative Fee:</span>
                <span className="font-extrabold text-blue-400">₹{getCollegeFee(service)}</span>
              </div>
            </div>

            <div className="space-y-2">
              <label className="font-bold text-slate-400">Select Payment Method</label>
              <select
                value={paymentMethod}
                onChange={e => setPaymentMethod(e.target.value)}
                className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-slate-200 focus:outline-none"
              >
                <option value="UPI">UPI Payment</option>
                <option value="DEBIT_CARD">Debit/Credit Card</option>
                <option value="NET_BANKING">Net Banking</option>
                <option value="QR_PAYMENT">QR Payment Scan</option>
              </select>
            </div>

            {paymentMethod === 'QR_PAYMENT' && (
              <div className="p-4 bg-slate-950/80 rounded-xl border border-slate-900/60 text-center space-y-2 animate-in fade-in duration-200">
                <div className="w-32 h-32 bg-white rounded-lg mx-auto flex items-center justify-center p-2 shadow-inner">
                  {/* Mock QR graphic */}
                  <div className="w-full h-full border-4 border-slate-950 flex flex-wrap p-1 gap-1">
                    <div className="w-8 h-8 bg-slate-950"></div>
                    <div className="w-8 h-8 bg-slate-950 ml-auto"></div>
                    <div className="w-12 h-12 bg-slate-950 mt-auto"></div>
                  </div>
                </div>
                <p className="text-[10px] text-slate-400 font-bold">Scan to Pay via BHIM / Any UPI App</p>
              </div>
            )}

            <div className="flex items-center space-x-3 pt-2">
              <button
                onClick={handlePaymentAndBook}
                disabled={paying}
                className="flex-1 py-3 bg-blue-600 hover:bg-blue-500 disabled:bg-slate-800 disabled:text-slate-500 text-white font-semibold rounded-xl flex items-center justify-center gap-2 shadow-md shadow-blue-500/10 cursor-pointer"
              >
                {paying ? 'Processing Transaction...' : 'Pay & Generate Token'}
              </button>
              <button
                onClick={() => setShowPayment(false)}
                disabled={paying}
                className="px-5 py-3 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 font-semibold rounded-xl cursor-pointer"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {paymentSuccessData && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 backdrop-blur-sm p-4 animate-in fade-in duration-200">
          <div className="glass-panel border border-slate-800 rounded-2xl p-8 max-w-md w-full space-y-6 text-center text-xs">
            <div className="w-16 h-16 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center mx-auto">
              <CheckCircle className="w-8 h-8 text-emerald-400" />
            </div>

            <div className="space-y-1">
              <h3 className="text-xl font-bold text-slate-100">Booking Confirmed!</h3>
              <p className="text-slate-400">
                {paymentSuccessData.transactionId === 'FREE_OF_COST' 
                  ? 'Your slot has been booked successfully' 
                  : 'Payment completed & slot booked successfully'}
              </p>
            </div>

            <div className="p-6 bg-slate-950/60 rounded-2xl border border-slate-900 space-y-3">
              <div>
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Your Ticket Number</span>
                <div className="text-4xl font-extrabold text-blue-400 mt-1">{paymentSuccessData.tokenNumber}</div>
              </div>
              <div className="border-t border-slate-900 pt-3 flex justify-between text-[11px]">
                <span className="text-slate-500">Transaction ID:</span>
                <span className="font-mono font-semibold text-slate-300">
                  {paymentSuccessData.transactionId === 'FREE_OF_COST' ? 'Free Service' : paymentSuccessData.transactionId}
                </span>
              </div>
            </div>

            <div className="space-y-3">
              {paymentSuccessData.transactionId !== 'FREE_OF_COST' && (
                <button
                  onClick={() => window.open(`${API_BASE}/payment/receipt/${paymentSuccessData.paymentId}`)}
                  className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-xl shadow-md shadow-blue-500/10 flex items-center justify-center gap-2 cursor-pointer"
                >
                  <FileText className="w-4 h-4" />
                  Download PDF Receipt
                </button>
              )}
              <button
                onClick={() => {
                  setPaymentSuccessData(null);
                  setShowPayment(false);
                  setPage('dashboard');
                }}
                className="w-full py-3 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 font-semibold rounded-xl cursor-pointer"
              >
                Go to Dashboard
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ----------------------------------------------------
// 8. STAFF PANEL
// ----------------------------------------------------
function StaffDashboard() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [serving, setServing] = useState(null);
  const [waiting, setWaiting] = useState([]);
  const [statusText, setStatusText] = useState('ACTIVE');
  const [syncing, setSyncing] = useState(false);

  const [showSwitchDesk, setShowSwitchDesk] = useState(false);
  const [deskSector, setDeskSector] = useState('');
  const [deskOrgs, setDeskOrgs] = useState([]);
  const [selectedDeskOrgId, setSelectedDeskOrgId] = useState('');
  const [deskBranches, setDeskBranches] = useState([]);
  const [selectedDeskBranchId, setSelectedDeskBranchId] = useState('');
  const [deskCounters, setDeskCounters] = useState([]);
  const [selectedDeskCounterId, setSelectedDeskCounterId] = useState('');

  const fetchQueue = async (refId, sect) => {
    setSyncing(true);
    try {
      const serveRes = await axios.get(`${API_BASE}/staff/queue/serving?sectorType=${sect}&referenceId=${refId}`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (serveRes.data.success) {
        setServing(serveRes.data.data);
      }

      const waitRes = await axios.get(`${API_BASE}/staff/queue/waiting?sectorType=${sect}&referenceId=${refId}`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (waitRes.data.success) {
        // Sort: Emergency first, then ID
        const sorted = waitRes.data.data.sort((a, b) => {
          if (a.priority === 'EMERGENCY' && b.priority !== 'EMERGENCY') return -1;
          if (a.priority !== 'EMERGENCY' && b.priority === 'EMERGENCY') return 1;
          return a.id - b.id;
        });
        setWaiting(sorted);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setSyncing(false);
    }
  };

  useEffect(() => {
    if (!deskSector) return;
    setSelectedDeskOrgId('');
    setSelectedDeskBranchId('');
    setSelectedDeskCounterId('');
    setDeskOrgs([]);
    setDeskBranches([]);
    setDeskCounters([]);

    const endpoint = deskSector === 'HOSPITAL' ? '/hospital/list' : (deskSector === 'BANK' ? '/bank/list' : '/college/list');
    axios.get(`${API_BASE}${endpoint}`, {
      headers: { Authorization: `Bearer ${user.token}` }
    }).then(res => {
      if (res.data.success) {
        setDeskOrgs(res.data.data);
      }
    }).catch(err => console.error(err));
  }, [deskSector]);

  useEffect(() => {
    if (!selectedDeskOrgId) return;
    setSelectedDeskBranchId('');
    setSelectedDeskCounterId('');
    setDeskBranches([]);
    setDeskCounters([]);

    const endpoint = deskSector === 'HOSPITAL' ? `/hospital/${selectedDeskOrgId}/branches` : (deskSector === 'BANK' ? `/bank/${selectedDeskOrgId}/branches` : `/college/${selectedDeskOrgId}/departments`);
    axios.get(`${API_BASE}${endpoint}`, {
      headers: { Authorization: `Bearer ${user.token}` }
    }).then(res => {
      if (res.data.success) {
        setDeskBranches(res.data.data);
      }
    }).catch(err => console.error(err));
  }, [selectedDeskOrgId]);

  useEffect(() => {
    if (!selectedDeskBranchId) return;
    setSelectedDeskCounterId('');
    setDeskCounters([]);

    axios.get(`${API_BASE}/staff/counters?sectorType=${deskSector}&branchId=${selectedDeskBranchId}`, {
      headers: { Authorization: `Bearer ${user.token}` }
    }).then(res => {
      if (res.data.success) {
        setDeskCounters(res.data.data);
      }
    }).catch(err => console.error(err));
  }, [selectedDeskBranchId]);

  const handleUpdateDeskSubmit = async (e) => {
    e.preventDefault();
    if (!deskSector || !selectedDeskBranchId || !selectedDeskCounterId) return;
    try {
      const res = await axios.post(`${API_BASE}/staff/update-counter?sectorType=${deskSector}&referenceId=${selectedDeskBranchId}&counterId=${selectedDeskCounterId}`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        // Re-fetch full profile to get updated branchName/organizationName
        const profRes = await axios.get(`${API_BASE}/staff/profile/${user.userId}`, {
          headers: { Authorization: `Bearer ${user.token}` }
        });
        if (profRes.data.success) {
          const updatedProf = profRes.data.data;
          setProfile(updatedProf);
          setStatusText(updatedProf.counter?.status || 'ACTIVE');
          setDeskSector(updatedProf.sectorType);
          setSelectedDeskBranchId(updatedProf.referenceId);
          if (updatedProf.counter) setSelectedDeskCounterId(updatedProf.counter.id);
        }
        setShowSwitchDesk(false);
        fetchQueue(selectedDeskBranchId, deskSector);
      }
    } catch (err) {
      alert("Failed to update desk!");
      console.error(err);
    }
  };

  useEffect(() => {
    let interval;
    // 1. Fetch Staff Profile
    axios.get(`${API_BASE}/staff/profile/${user.userId}`, {
      headers: { Authorization: `Bearer ${user.token}` }
    }).then(res => {
      if (res.data.success) {
        const prof = res.data.data;
        setProfile(prof);
        setStatusText(prof.counter?.status || 'ACTIVE');
        
        // Lock and initialize the switch desk state to staff's assigned values
        setDeskSector(prof.sectorType);
        setSelectedDeskBranchId(prof.referenceId);
        if (prof.counter) {
          setSelectedDeskCounterId(prof.counter.id);
        }

        // Fetch all counters belonging to their pre-assigned branch/campus
        axios.get(`${API_BASE}/staff/counters?sectorType=${prof.sectorType}&branchId=${prof.referenceId}`, {
          headers: { Authorization: `Bearer ${user.token}` }
        }).then(cRes => {
          if (cRes.data.success) {
            setDeskCounters(cRes.data.data);
          }
        }).catch(cErr => console.error(cErr));

        // 2. Fetch Queues
        fetchQueue(prof.referenceId, prof.sectorType);

        // Start live short-polling every 4 seconds
        interval = setInterval(() => {
          fetchQueue(prof.referenceId, prof.sectorType);
        }, 4000);
      }
    }).catch(err => console.error(err));

    return () => {
      if (interval) clearInterval(interval);
    };
  }, []);

  const handleCallNext = async () => {
    if (!profile) return;
    try {
      const res = await axios.post(`${API_BASE}/staff/queue/call-next?sectorType=${profile.sectorType}&referenceId=${profile.referenceId}`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        setServing(res.data.data);
        fetchQueue(profile.referenceId, profile.sectorType);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleComplete = async (tokenId) => {
    try {
      const res = await axios.post(`${API_BASE}/staff/token/${tokenId}/complete`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        setServing(null);
        fetchQueue(profile.referenceId, profile.sectorType);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleSkip = async (tokenId) => {
    try {
      const res = await axios.post(`${API_BASE}/staff/token/${tokenId}/skip`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        setServing(null);
        fetchQueue(profile.referenceId, profile.sectorType);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleAcceptEmergency = async (tokenId) => {
    try {
      const res = await axios.post(`${API_BASE}/staff/queue/call-specific?tokenId=${tokenId}`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        setServing(res.data.data);
        fetchQueue(profile.referenceId, profile.sectorType);
      }
    } catch (err) {
      alert("Failed to accept emergency token!");
      console.error(err);
    }
  };

  const handleDeclineEmergency = async (tokenId) => {
    if (!window.confirm("Are you sure you want to decline (cancel) this emergency token?")) return;
    try {
      const res = await axios.post(`${API_BASE}/token/${tokenId}/cancel`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        fetchQueue(profile.referenceId, profile.sectorType);
      }
    } catch (err) {
      alert("Failed to decline emergency token!");
      console.error(err);
    }
  };

  const handleSkipEmergency = async (tokenId) => {
    try {
      const res = await axios.post(`${API_BASE}/staff/token/${tokenId}/skip`, {}, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        fetchQueue(profile.referenceId, profile.sectorType);
      }
    } catch (err) {
      alert("Failed to skip emergency token!");
      console.error(err);
    }
  };

  if (!profile) {
    return <div className="text-center py-12 text-slate-400">Loading staff operations...</div>;
  }

  return (
    <div className="space-y-8">
      {/* Profile summary header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-900/60 p-6 rounded-2xl border border-slate-800">
        <div>
          <span className="text-[10px] font-bold uppercase tracking-wider text-blue-500">
            {profile.sectorType === 'HOSPITAL' ? 'Hospital Staff Dashboard' : (profile.sectorType === 'BANK' ? 'Bank Staff Dashboard' : 'College Staff Dashboard')}
          </span>
          <h2 className="text-3xl font-extrabold">{profile.counter?.counterName || profile.counter?.name || 'Assigned Counter'}</h2>
          <p className="text-xs text-slate-400 mt-1">
            Operator: <span className="text-slate-200 font-semibold">{user.fullName}</span>
            {(profile.organizationName || profile.branchName) && (
              <span className="ml-2 text-slate-500">
                {profile.organizationName && <span className="text-emerald-400">{profile.organizationName}</span>}
                {profile.branchName && <span> • {profile.branchName}</span>}
              </span>
            )}
          </p>
        </div>
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2">
            <span className={`w-3.5 h-3.5 rounded-full ${statusText === 'ACTIVE' ? 'bg-emerald-500' : 'bg-rose-500'}`}></span>
            <span className="text-xs font-bold uppercase tracking-wider text-slate-300">Desk Status: {statusText}</span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Core Serving Controls */}
        <div className="lg:col-span-2 space-y-6">
          <div className="glass-panel border border-slate-800 rounded-2xl p-6 md:p-8 space-y-6 text-center">
            <span className="text-xs text-slate-500 font-bold uppercase tracking-widest">Active Serving Ticket</span>
            {serving ? (
              <div className="space-y-4">
                <div className="text-7xl font-extrabold bg-gradient-to-r from-blue-500 to-indigo-400 bg-clip-text text-transparent">
                  {serving.tokenNumber}
                </div>
                <div className="text-sm font-semibold text-slate-300">
                  User: {serving.user.fullName} | Service: {serving.serviceName}
                </div>
                <div className="flex items-center justify-center space-x-4 pt-2">
                  <button 
                    onClick={() => handleComplete(serving.id)}
                    className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-xs font-semibold rounded-xl text-white transition-colors"
                  >
                    Mark Served (Complete)
                  </button>
                  <button 
                    onClick={() => handleSkip(serving.id)}
                    className="px-5 py-2.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold rounded-xl text-rose-400 transition-colors"
                  >
                    Skip Token
                  </button>
                </div>
              </div>
            ) : (
              <div className="py-8 space-y-4">
                <Clock className="w-12 h-12 text-slate-600 mx-auto" />
                <p className="text-xs text-slate-500 max-w-sm mx-auto">No token is currently called to your desk. Invite the next token in line to begin service.</p>
                <button 
                  onClick={handleCallNext}
                  className="px-6 py-3 bg-blue-600 hover:bg-blue-500 text-xs font-semibold text-white rounded-xl shadow-lg shadow-blue-500/20"
                >
                  Call Next Ticket
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Live waiting queue list */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-bold">Waiting Queue ({waiting.length})</h3>
            <button 
              onClick={() => fetchQueue(profile.referenceId, profile.sectorType)}
              disabled={syncing}
              className="text-[10px] text-blue-500 hover:underline flex items-center space-x-1"
            >
              <RefreshCw className={`w-3 h-3 ${syncing ? 'animate-spin' : ''}`} />
              <span>Refresh</span>
            </button>
          </div>

          <div className="glass-panel border border-slate-800 rounded-2xl p-4 max-h-[300px] overflow-y-auto space-y-2">
            {waiting.length === 0 ? (
              <p className="text-xs text-slate-500 text-center py-8">Queue is completely empty</p>
            ) : (
              waiting.map((t, idx) => (
                <div key={t.id} className="flex items-center justify-between p-3 rounded-xl bg-slate-900 border border-slate-800/80">
                  <div>
                    <div className="font-bold text-sm text-blue-400">{t.tokenNumber}</div>
                    <div className="text-[10px] text-slate-400 mt-0.5">{t.serviceName}</div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <span className={`px-2 py-0.5 rounded text-[8px] font-bold border ${t.priority === 'EMERGENCY' ? 'bg-rose-500/10 border-rose-500/20 text-rose-400' : 'bg-slate-800 border-slate-700 text-slate-400'}`}>
                      {t.priority}
                    </span>
                    <span className="text-[10px] text-slate-500 mr-2">#{idx + 1} in line</span>
                    <div className="flex items-center space-x-1 shrink-0">
                      <button
                        onClick={() => handleAcceptEmergency(t.id)}
                        className="px-2 py-1 bg-emerald-600 hover:bg-emerald-500 text-[10px] font-bold text-white rounded transition-colors cursor-pointer"
                      >
                        Accept
                      </button>
                      <button
                        onClick={() => handleDeclineEmergency(t.id)}
                        className="px-2 py-1 bg-rose-600 hover:bg-rose-500 text-[10px] font-bold text-white rounded transition-colors cursor-pointer"
                      >
                        Decline
                      </button>
                      <button
                        onClick={() => handleSkipEmergency(t.id)}
                        className="px-2 py-1 bg-slate-800 hover:bg-slate-700 text-[10px] font-bold text-slate-300 rounded border border-slate-700 transition-colors cursor-pointer"
                      >
                        Skip
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// ----------------------------------------------------
// 9. ADMIN PANEL
// ----------------------------------------------------
function AdminDashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  
  // States for CRUD lists
  const [hospitalsList, setHospitalsList] = useState([]);
  const [banksList, setBanksList] = useState([]);
  const [collegesList, setCollegesList] = useState([]);
  const [adminTab, setAdminTab] = useState('hospital');

  // States for CRUD forms
  const [hospForm, setHospForm] = useState({ name: '', logoUrl: '' });
  const [bankForm, setBankForm] = useState({ name: '', logoUrl: '' });
  const [collegeForm, setCollegeForm] = useState({ name: '', logoUrl: '' });

  const fetchStats = async () => {
    try {
      const res = await axios.get(`${API_BASE}/admin/stats`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        setStats(res.data.data);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const fetchLists = async () => {
    try {
      const hRes = await axios.get(`${API_BASE}/admin/hospitals`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (hRes.data.success) setHospitalsList(hRes.data.data);

      const bRes = await axios.get(`${API_BASE}/admin/banks`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (bRes.data.success) setBanksList(bRes.data.data);

      const cRes = await axios.get(`${API_BASE}/admin/colleges`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (cRes.data.success) setCollegesList(cRes.data.data);
    } catch (err) {
      console.error("Error fetching administrative lists:", err);
    }
  };

  const handleExport = async (endpoint, filename) => {
    try {
      const res = await axios.get(`${API_BASE}${endpoint}`, {
        headers: { Authorization: `Bearer ${user.token}` },
        responseType: 'blob'
      });
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert("Failed to export data: " + (err.response?.data?.message || err.message));
    }
  };

  useEffect(() => {
    fetchStats();
    fetchLists();
    // Live short-polling every 4 seconds
    const interval = setInterval(() => {
      fetchStats();
      fetchLists();
    }, 4000);
    return () => clearInterval(interval);
  }, []);

  const handleAddHosp = async (e) => {
    e.preventDefault();
    try {
      await axios.post(`${API_BASE}/admin/hospital`, hospForm, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      alert("Hospital added successfully!");
      setHospForm({ name: '', logoUrl: '' });
      fetchStats();
      fetchLists();
    } catch (err) {
      console.error(err);
    }
  };

  const handleAddBank = async (e) => {
    e.preventDefault();
    try {
      await axios.post(`${API_BASE}/admin/bank`, bankForm, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      alert("Bank added successfully!");
      setBankForm({ name: '', logoUrl: '' });
      fetchStats();
      fetchLists();
    } catch (err) {
      console.error(err);
    }
  };

  const handleAddCol = async (e) => {
    e.preventDefault();
    try {
      await axios.post(`${API_BASE}/admin/college`, collegeForm, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      alert("College added successfully!");
      setCollegeForm({ name: '', logoUrl: '' });
      fetchStats();
      fetchLists();
    } catch (err) {
      console.error(err);
    }
  };

  const handleDeleteHosp = async (id) => {
    if (!window.confirm("Are you sure you want to delete this hospital?")) return;
    try {
      await axios.delete(`${API_BASE}/admin/hospital/${id}`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      alert("Hospital deleted successfully!");
      fetchStats();
      fetchLists();
    } catch (err) {
      alert("Failed to delete hospital: " + (err.response?.data?.message || err.message));
    }
  };

  const handleDeleteBank = async (id) => {
    if (!window.confirm("Are you sure you want to delete this bank?")) return;
    try {
      await axios.delete(`${API_BASE}/admin/bank/${id}`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      alert("Bank deleted successfully!");
      fetchStats();
      fetchLists();
    } catch (err) {
      alert("Failed to delete bank: " + (err.response?.data?.message || err.message));
    }
  };

  const handleDeleteCol = async (id) => {
    if (!window.confirm("Are you sure you want to delete this college?")) return;
    try {
      await axios.delete(`${API_BASE}/admin/college/${id}`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      alert("College deleted successfully!");
      fetchStats();
      fetchLists();
    } catch (err) {
      alert("Failed to delete college: " + (err.response?.data?.message || err.message));
    }
  };

  if (!stats) {
    return <div className="text-center py-12 text-slate-400">Loading system statistics...</div>;
  }

  return (
    <div className="space-y-10">
      <div className="pb-4 border-b border-slate-900">
        <h2 className="text-3xl font-extrabold tracking-tight">Admin Control Panel</h2>
        <p className="text-xs text-slate-400 mt-1">Cross-system diagnostics, system metrics, and department CRUD tools.</p>
      </div>

      {/* Aggregate widgets */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="glass-card rounded-2xl p-5 border border-slate-800/80">
          <div className="text-slate-500 text-[10px] uppercase font-bold tracking-wider">Total Users</div>
          <div className="text-3xl font-extrabold text-blue-400 mt-1">{stats.totalUsers}</div>
        </div>

        <div className="glass-card rounded-2xl p-5 border border-slate-800/80">
          <div className="text-slate-500 text-[10px] uppercase font-bold tracking-wider">Tokens Generated</div>
          <div className="text-3xl font-extrabold text-indigo-400 mt-1">{stats.totalTokens}</div>
        </div>

        <div className="glass-card rounded-2xl p-5 border border-slate-800/80">
          <div className="text-slate-500 text-[10px] uppercase font-bold tracking-wider">Active Counters</div>
          <div className="text-3xl font-extrabold text-violet-400 mt-1">{stats.totalCounters}</div>
        </div>

        <div className="glass-card rounded-2xl p-5 border border-slate-800/80">
          <div className="text-slate-500 text-[10px] uppercase font-bold tracking-wider">Avg Wait Times</div>
          <div className="text-3xl font-extrabold text-amber-400 mt-1">{stats.averageWaitTime} mins</div>
        </div>
      </div>

      {/* Sector Counts */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="glass-card rounded-2xl p-5 border border-slate-800/80 flex items-center justify-between">
          <div>
            <div className="text-slate-500 text-[10px] uppercase font-bold tracking-wider">Active Hospitals</div>
            <div className="text-2xl font-extrabold text-rose-400 mt-1">{stats.hospitalCount || 0}</div>
          </div>
          <Activity className="w-8 h-8 text-rose-500/20" />
        </div>

        <div className="glass-card rounded-2xl p-5 border border-slate-800/80 flex items-center justify-between">
          <div>
            <div className="text-slate-500 text-[10px] uppercase font-bold tracking-wider">Active Banks</div>
            <div className="text-2xl font-extrabold text-blue-400 mt-1">{stats.bankCount || 0}</div>
          </div>
          <Building className="w-8 h-8 text-blue-500/20" />
        </div>

        <div className="glass-card rounded-2xl p-5 border border-slate-800/80 flex items-center justify-between">
          <div>
            <div className="text-slate-500 text-[10px] uppercase font-bold tracking-wider">Active Colleges</div>
            <div className="text-2xl font-extrabold text-violet-400 mt-1">{stats.collegeCount || 0}</div>
          </div>
          <BookOpen className="w-8 h-8 text-violet-500/20" />
        </div>
      </div>

      {/* Visual charts simulation */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div className="glass-panel border border-slate-800 rounded-2xl p-6 space-y-4">
          <h3 className="text-sm font-bold uppercase tracking-wider text-slate-400">Category Distributions</h3>
          <div className="space-y-3 pt-2">
            <div className="space-y-1">
              <div className="flex items-center justify-between text-xs">
                <span>Hospitals</span>
                <span className="font-semibold text-rose-400">{stats.hospitalTokens}</span>
              </div>
              <div className="w-full h-2.5 bg-slate-900 rounded-full overflow-hidden">
                <div className="h-full bg-rose-500 rounded-full" style={{ width: `${stats.totalTokens > 0 ? (stats.hospitalTokens / stats.totalTokens) * 100 : 0}%` }}></div>
              </div>
            </div>

            <div className="space-y-1">
              <div className="flex items-center justify-between text-xs">
                <span>Banks</span>
                <span className="font-semibold text-blue-400">{stats.bankTokens}</span>
              </div>
              <div className="w-full h-2.5 bg-slate-900 rounded-full overflow-hidden">
                <div className="h-full bg-blue-500 rounded-full" style={{ width: `${stats.totalTokens > 0 ? (stats.bankTokens / stats.totalTokens) * 100 : 0}%` }}></div>
              </div>
            </div>

            <div className="space-y-1">
              <div className="flex items-center justify-between text-xs">
                <span>Colleges</span>
                <span className="font-semibold text-violet-400">{stats.collegeTokens}</span>
              </div>
              <div className="w-full h-2.5 bg-slate-900 rounded-full overflow-hidden">
                <div className="h-full bg-violet-500 rounded-full" style={{ width: `${stats.totalTokens > 0 ? (stats.collegeTokens / stats.totalTokens) * 100 : 0}%` }}></div>
              </div>
            </div>
          </div>
        </div>

        {/* Dynamic CRUD insertion and list management console */}
        <div className="glass-panel border border-slate-800 rounded-2xl p-6 space-y-6">
          <div className="flex items-center justify-between border-b border-slate-800 pb-3">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-400">Organization Console</h3>
            <div className="flex bg-slate-950 p-1 rounded-xl border border-slate-800/80 text-[10px] font-bold">
              <button 
                onClick={() => setAdminTab('hospital')}
                className={`px-3 py-1.5 rounded-lg transition-colors ${adminTab === 'hospital' ? 'bg-rose-500/20 text-rose-400' : 'text-slate-400 hover:text-slate-200'}`}
              >
                Hospitals
              </button>
              <button 
                onClick={() => setAdminTab('bank')}
                className={`px-3 py-1.5 rounded-lg transition-colors ${adminTab === 'bank' ? 'bg-blue-500/20 text-blue-400' : 'text-slate-400 hover:text-slate-200'}`}
              >
                Banks
              </button>
              <button 
                onClick={() => setAdminTab('college')}
                className={`px-3 py-1.5 rounded-lg transition-colors ${adminTab === 'college' ? 'bg-violet-500/20 text-violet-400' : 'text-slate-400 hover:text-slate-200'}`}
              >
                Colleges
              </button>
            </div>
          </div>

          <div className="space-y-4 text-xs">
            {/* List entries */}
            <div className="space-y-2 max-h-[160px] overflow-y-auto pr-1">
              {adminTab === 'hospital' && (
                hospitalsList.length === 0 ? (
                  <div className="text-[11px] text-slate-500 italic py-2">No hospitals registered</div>
                ) : (
                  hospitalsList.map(h => (
                    <div key={h.id} className="flex items-center justify-between bg-slate-950/40 p-2.5 rounded-xl border border-slate-900">
                      <span className="font-medium text-slate-300 truncate max-w-[200px]">{h.name}</span>
                      <button 
                        onClick={() => handleDeleteHosp(h.id)}
                        className="p-1 hover:bg-rose-500/10 text-rose-400 hover:text-rose-300 rounded-lg transition-colors"
                      >
                        <Trash className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))
                )
              )}

              {adminTab === 'bank' && (
                banksList.length === 0 ? (
                  <div className="text-[11px] text-slate-500 italic py-2">No banks registered</div>
                ) : (
                  banksList.map(b => (
                    <div key={b.id} className="flex items-center justify-between bg-slate-950/40 p-2.5 rounded-xl border border-slate-900">
                      <span className="font-medium text-slate-300 truncate max-w-[200px]">{b.name}</span>
                      <button 
                        onClick={() => handleDeleteBank(b.id)}
                        className="p-1 hover:bg-rose-500/10 text-rose-400 hover:text-rose-300 rounded-lg transition-colors"
                      >
                        <Trash className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))
                )
              )}

              {adminTab === 'college' && (
                collegesList.length === 0 ? (
                  <div className="text-[11px] text-slate-500 italic py-2">No colleges registered</div>
                ) : (
                  collegesList.map(c => (
                    <div key={c.id} className="flex items-center justify-between bg-slate-950/40 p-2.5 rounded-xl border border-slate-900">
                      <span className="font-medium text-slate-300 truncate max-w-[200px]">{c.name}</span>
                      <button 
                        onClick={() => handleDeleteCol(c.id)}
                        className="p-1 hover:bg-rose-500/10 text-rose-400 hover:text-rose-300 rounded-lg transition-colors"
                      >
                        <Trash className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))
                )
              )}
            </div>

            {/* Insertion Form for active tab */}
            <div className="pt-3 border-t border-slate-800">
              {adminTab === 'hospital' && (
                <form onSubmit={handleAddHosp} className="flex items-center space-x-2">
                  <input 
                    type="text" 
                    required
                    value={hospForm.name}
                    onChange={e => setHospForm({ ...hospForm, name: e.target.value })}
                    placeholder="New Hospital Name" 
                    className="flex-1 bg-slate-900 border border-slate-850 focus:border-rose-500 rounded-xl px-3.5 py-2.5 focus:outline-none transition-colors"
                  />
                  <button type="submit" className="px-3.5 py-2.5 bg-rose-600 hover:bg-rose-500 text-white rounded-xl font-semibold flex items-center space-x-1 shrink-0">
                    <Plus className="w-3.5 h-3.5" />
                    <span>Hospital</span>
                  </button>
                </form>
              )}

              {adminTab === 'bank' && (
                <form onSubmit={handleAddBank} className="flex items-center space-x-2">
                  <input 
                    type="text" 
                    required
                    value={bankForm.name}
                    onChange={e => setBankForm({ ...bankForm, name: e.target.value })}
                    placeholder="New Bank Name" 
                    className="flex-1 bg-slate-900 border border-slate-850 focus:border-blue-500 rounded-xl px-3.5 py-2.5 focus:outline-none transition-colors"
                  />
                  <button type="submit" className="px-3.5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-semibold flex items-center space-x-1 shrink-0">
                    <Plus className="w-3.5 h-3.5" />
                    <span>Bank</span>
                  </button>
                </form>
              )}

              {adminTab === 'college' && (
                <form onSubmit={handleAddCol} className="flex items-center space-x-2">
                  <input 
                    type="text" 
                    required
                    value={collegeForm.name}
                    onChange={e => setCollegeForm({ ...collegeForm, name: e.target.value })}
                    placeholder="New College Name" 
                    className="flex-1 bg-slate-900 border border-slate-850 focus:border-violet-500 rounded-xl px-3.5 py-2.5 focus:outline-none transition-colors"
                  />
                  <button type="submit" className="px-3.5 py-2.5 bg-violet-600 hover:bg-violet-500 text-white rounded-xl font-semibold flex items-center space-x-1 shrink-0">
                    <Plus className="w-3.5 h-3.5" />
                    <span>College</span>
                  </button>
                </form>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* System Reports & CSV Export Utilities */}
      <div className="glass-panel border border-slate-800 rounded-2xl p-6 space-y-6 shadow-xl">
        <div>
          <h3 className="text-lg font-bold tracking-tight bg-gradient-to-r from-white to-slate-400 bg-clip-text text-transparent flex items-center space-x-2">
            <FileText className="w-5 h-5 text-blue-500" />
            <span>System Reports & CSV Export Utilities</span>
          </h3>
          <p className="text-xs text-slate-400 mt-1">Export high-fidelity, real-time database logs, user metrics, and activity archives.</p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-xs">
          <button 
            onClick={() => handleExport('/admin/export/users', 'users_export.csv')}
            className="flex items-center justify-between p-4 bg-slate-900 hover:bg-slate-800 border border-slate-800/80 rounded-xl font-semibold text-slate-200 transition-all group hover:scale-[1.02] cursor-pointer"
          >
            <span>Export Users CSV</span>
            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-blue-400 transition-colors" />
          </button>

          <button 
            onClick={() => handleExport('/admin/export/appointments', 'appointments_export.csv')}
            className="flex items-center justify-between p-4 bg-slate-900 hover:bg-slate-800 border border-slate-800/80 rounded-xl font-semibold text-slate-200 transition-all group hover:scale-[1.02] cursor-pointer"
          >
            <span>Export Appointments CSV</span>
            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-emerald-400 transition-colors" />
          </button>

          <button 
            onClick={() => handleExport('/admin/export/tokens', 'tokens_export.csv')}
            className="flex items-center justify-between p-4 bg-slate-900 hover:bg-slate-800 border border-slate-800/80 rounded-xl font-semibold text-slate-200 transition-all group hover:scale-[1.02] cursor-pointer"
          >
            <span>Export Token History CSV</span>
            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-indigo-400 transition-colors" />
          </button>

          <button 
            onClick={() => handleExport('/admin/export/queues', 'queues_export.csv')}
            className="flex items-center justify-between p-4 bg-slate-900 hover:bg-slate-800 border border-slate-800/80 rounded-xl font-semibold text-slate-200 transition-all group hover:scale-[1.02] cursor-pointer"
          >
            <span>Export Active Queues CSV</span>
            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-rose-400 transition-colors" />
          </button>

          <button 
            onClick={() => handleExport('/admin/export/hospitals', 'hospitals_export.csv')}
            className="flex items-center justify-between p-4 bg-slate-900 hover:bg-slate-800 border border-slate-800/80 rounded-xl font-semibold text-slate-200 transition-all group hover:scale-[1.02] cursor-pointer"
          >
            <span>Export Hospitals CSV</span>
            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-rose-400 transition-colors" />
          </button>

          <button 
            onClick={() => handleExport('/admin/export/banks', 'banks_export.csv')}
            className="flex items-center justify-between p-4 bg-slate-900 hover:bg-slate-800 border border-slate-800/80 rounded-xl font-semibold text-slate-200 transition-all group hover:scale-[1.02] cursor-pointer"
          >
            <span>Export Banks CSV</span>
            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-blue-400 transition-colors" />
          </button>

          <button 
            onClick={() => handleExport('/admin/export/colleges', 'colleges_export.csv')}
            className="flex items-center justify-between p-4 bg-slate-900 hover:bg-slate-800 border border-slate-800/80 rounded-xl font-semibold text-slate-200 transition-all group hover:scale-[1.02] cursor-pointer"
          >
            <span>Export Colleges CSV</span>
            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-violet-400 transition-colors" />
          </button>

          <button 
            onClick={() => handleExport('/admin/export/notifications', 'notifications_export.csv')}
            className="flex items-center justify-between p-4 bg-slate-900 hover:bg-slate-800 border border-slate-800/80 rounded-xl font-semibold text-slate-200 transition-all group hover:scale-[1.02] cursor-pointer"
          >
            <span>Export Notifications CSV</span>
            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-amber-400 transition-colors" />
          </button>

          <button 
            onClick={() => handleExport('/admin/export/staff', 'staff_export.csv')}
            className="flex items-center justify-between p-4 bg-slate-900 hover:bg-slate-800 border border-slate-800/80 rounded-xl font-semibold text-slate-200 transition-all group hover:scale-[1.02] cursor-pointer"
          >
            <span>Export Staff Profiles CSV</span>
            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-emerald-400 transition-colors" />
          </button>

          <button 
            onClick={() => handleExport('/admin/export/analytics', 'analytics_summary_export.csv')}
            className="flex items-center justify-between p-4 bg-slate-900 hover:bg-slate-800 border border-slate-800/80 rounded-xl font-semibold text-slate-200 transition-all group hover:scale-[1.02] cursor-pointer"
          >
            <span>Export Analytics Summary CSV</span>
            <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-violet-400 transition-colors" />
          </button>
        </div>
      </div>
    </div>
  );
}

// ----------------------------------------------------
// 10. PROFILE PAGE
// ----------------------------------------------------
function ProfilePage({ setPage }) {
  const { user, login } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [form, setForm] = useState({ fullName: '', email: '', phoneNumber: '', profileImage: '' });
  const [pwForm, setPwForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const [pwError, setPwError] = useState('');
  const [pwSuccess, setPwSuccess] = useState('');
  const [pwLoading, setPwLoading] = useState(false);

  const [payments, setPayments] = useState([]);
  const [tokens, setTokens] = useState([]);

  const fetchProfile = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await axios.get(`${API_BASE}/users/profile`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        setProfile(res.data.data);
        setForm({
          fullName: res.data.data.fullName || '',
          email: res.data.data.email || '',
          phoneNumber: res.data.data.phoneNumber || '',
          profileImage: res.data.data.profileImage || ''
        });
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load profile details.');
    } finally {
      setLoading(false);
    }
  };

  const fetchHistory = async () => {
    try {
      const payRes = await axios.get(`${API_BASE}/payment/history/${user.userId}`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (payRes.data.success) setPayments(payRes.data.data);

      const tokRes = await axios.get(`${API_BASE}/token/user/${user.userId}`, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (tokRes.data.success) setTokens(tokRes.data.data);
    } catch (e) {
      console.error("Failed to fetch history:", e);
    }
  };

  useEffect(() => {
    fetchProfile();
    fetchHistory();
  }, []);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (file.size > 800 * 1024) {
      alert("Image is too large! Please choose an image smaller than 800 KB.");
      return;
    }

    const reader = new FileReader();
    reader.onloadend = () => {
      setForm(prev => ({ ...prev, profileImage: reader.result }));
    };
    reader.readAsDataURL(file);
  };

  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const res = await axios.put(`${API_BASE}/users/profile/update`, form, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        setProfile(res.data.data);
        setSuccess('Profile updated successfully!');
        setEditMode(false);
        // Sync local storage and state for auth navbar updates!
        login({
          ...user,
          fullName: res.data.data.fullName,
          email: res.data.data.email,
          profileImage: res.data.data.profileImage
        });
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update profile!');
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      setPwError('New passwords do not match!');
      return;
    }
    setPwLoading(true);
    setPwError('');
    setPwSuccess('');
    try {
      const res = await axios.put(`${API_BASE}/users/change-password`, {
        oldPassword: pwForm.oldPassword,
        newPassword: pwForm.newPassword
      }, {
        headers: { Authorization: `Bearer ${user.token}` }
      });
      if (res.data.success) {
        setPwSuccess('Password updated successfully!');
        setPwForm({ oldPassword: '', newPassword: '', confirmPassword: '' });
      }
    } catch (err) {
      setPwError(err.response?.data?.message || 'Failed to change password! Check your current password.');
    } finally {
      setPwLoading(false);
    }
  };

  if (loading && !profile) {
    return <div className="text-center py-12 text-slate-400">Loading profile details...</div>;
  }

  return (
    <div className="space-y-8">
      {/* Header section */}
      <div className="flex items-center justify-between pb-4 border-b border-slate-900">
        <div>
          <h2 className="text-3xl font-extrabold tracking-tight">Account Profile</h2>
          <p className="text-sm text-slate-400 mt-1">Manage credentials, contact info, and custom avatar assets.</p>
        </div>
        <button 
          onClick={() => setPage(user.role === 'ROLE_ADMIN' ? 'admin' : (user.role === 'ROLE_STAFF' ? 'staff' : 'dashboard'))}
          className="px-4 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold rounded-xl text-slate-300 transition-colors cursor-pointer"
        >
          Back to Dashboard
        </button>
      </div>

      {error && (
        <div className="p-3.5 bg-rose-500/10 border border-rose-500/20 text-rose-400 rounded-xl text-xs flex items-center space-x-2">
          <AlertTriangle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="p-3.5 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-xl text-xs flex items-center space-x-2 animate-bounce">
          <CheckCircle className="w-4 h-4 shrink-0" />
          <span>{success}</span>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Side: Avatar Card */}
        <div className="glass-panel border border-slate-800 rounded-2xl p-6 text-center space-y-6 flex flex-col items-center justify-center">
          <div className="relative group">
            {form.profileImage ? (
              <img 
                src={form.profileImage} 
                alt="Avatar" 
                className="w-32 h-32 rounded-2xl object-cover border-2 border-blue-500/40 shadow-xl"
              />
            ) : (
              <div className="w-32 h-32 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white flex items-center justify-center font-bold text-4xl shadow-xl shadow-blue-500/15">
                {profile ? profile.fullName.substring(0, 2).toUpperCase() : '??'}
              </div>
            )}
            {editMode && (
              <label className="absolute inset-0 bg-black/60 rounded-2xl flex flex-col items-center justify-center text-[10px] font-bold text-slate-200 cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity border border-blue-500/50">
                <Plus className="w-5 h-5 mb-1 text-blue-400" />
                <span>Upload Photo</span>
                <input 
                  type="file" 
                  accept="image/*" 
                  onChange={handleImageChange}
                  className="hidden" 
                />
              </label>
            )}
          </div>

          <div className="space-y-1">
            <h3 className="text-xl font-bold">{profile ? profile.fullName : 'Loading...'}</h3>
            <p className="text-xs text-slate-400">{profile ? profile.email : 'Loading...'}</p>
          </div>

          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-blue-500/10 border border-blue-500/20 text-xs font-semibold text-blue-400 capitalize">
            <Shield className="w-3.5 h-3.5" />
            <span>{profile ? profile.role.replace('ROLE_', '').toLowerCase() : 'loading'}</span>
          </div>

          <div className="w-full border-t border-slate-800/80 pt-4 text-xs text-slate-400 space-y-2 text-left">
            <div className="flex justify-between">
              <span className="text-slate-500">Username</span>
              <span className="font-semibold text-slate-200">{profile ? profile.username : ''}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Phone Number</span>
              <span className="font-semibold text-slate-200">{profile?.phoneNumber || 'Not provided'}</span>
            </div>
          </div>
        </div>

        {/* Right Side: Profile Details Form */}
        <div className="lg:col-span-2 space-y-8">
          <div className="glass-panel border border-slate-800 rounded-2xl p-6 md:p-8 space-y-6">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800/80">
              <h3 className="text-lg font-bold">Profile Details</h3>
              <button 
                onClick={() => setEditMode(!editMode)}
                className="px-3.5 py-1.5 text-xs font-semibold border border-slate-700 rounded-lg hover:bg-slate-800 text-slate-300 transition-colors cursor-pointer"
              >
                {editMode ? 'Cancel Edit' : 'Edit Profile'}
              </button>
            </div>

            {editMode ? (
              <form onSubmit={handleProfileSubmit} className="space-y-4 text-xs">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="font-bold text-slate-400">Full Name</label>
                    <input 
                      type="text" 
                      required
                      value={form.fullName}
                      onChange={e => setForm({ ...form, fullName: e.target.value })}
                      className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl px-4 py-3 text-xs focus:outline-none transition-colors"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="font-bold text-slate-400">Email Address</label>
                    <input 
                      type="email" 
                      required
                      value={form.email}
                      onChange={e => setForm({ ...form, email: e.target.value })}
                      className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl px-4 py-3 text-xs focus:outline-none transition-colors"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="font-bold text-slate-400">Phone Number</label>
                  <input 
                    type="text" 
                    value={form.phoneNumber}
                    onChange={e => setForm({ ...form, phoneNumber: e.target.value })}
                    placeholder="Enter phone number"
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl px-4 py-3 text-xs focus:outline-none transition-colors"
                  />
                </div>

                <div className="flex items-center space-x-3 pt-2">
                  <button 
                    type="submit" 
                    disabled={loading}
                    className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-xl transition-all shadow-md shadow-blue-500/20 cursor-pointer"
                  >
                    Save Changes
                  </button>
                  <button 
                    type="button" 
                    onClick={() => setEditMode(false)}
                    className="px-5 py-2.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 font-semibold rounded-xl transition-colors cursor-pointer"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            ) : (
              <div className="space-y-4 text-xs">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-1 bg-slate-900/40 p-4 rounded-xl border border-slate-800/80">
                    <div className="text-slate-500 font-bold uppercase tracking-wider text-[9px]">Full Name</div>
                    <div className="text-sm font-semibold text-slate-200">{profile?.fullName}</div>
                  </div>
                  <div className="space-y-1 bg-slate-900/40 p-4 rounded-xl border border-slate-800/80">
                    <div className="text-slate-500 font-bold uppercase tracking-wider text-[9px]">Email Address</div>
                    <div className="text-sm font-semibold text-slate-200">{profile?.email}</div>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-1 bg-slate-900/40 p-4 rounded-xl border border-slate-800/80">
                    <div className="text-slate-500 font-bold uppercase tracking-wider text-[9px]">Phone Number</div>
                    <div className="text-sm font-semibold text-slate-200">{profile?.phoneNumber || 'Not specified'}</div>
                  </div>
                  <div className="space-y-1 bg-slate-900/40 p-4 rounded-xl border border-slate-800/80">
                    <div className="text-slate-500 font-bold uppercase tracking-wider text-[9px]">Authentication Role</div>
                    <div className="text-sm font-semibold text-slate-200 capitalize">{profile?.role.replace('ROLE_', '').toLowerCase()}</div>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Change Password Card */}
          <div className="glass-panel border border-slate-800 rounded-2xl p-6 md:p-8 space-y-6">
            <h3 className="text-lg font-bold pb-3 border-b border-slate-800/80">Credentials & Security</h3>

            {pwError && (
              <div className="p-3.5 bg-rose-500/10 border border-rose-500/20 text-rose-400 rounded-xl text-xs flex items-center space-x-2">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                <span>{pwError}</span>
              </div>
            )}

            {pwSuccess && (
              <div className="p-3.5 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-xl text-xs flex items-center space-x-2">
                <CheckCircle className="w-4 h-4 shrink-0" />
                <span>{pwSuccess}</span>
              </div>
            )}

            <form onSubmit={handlePasswordSubmit} className="space-y-4 text-xs">
              <div className="space-y-1.5">
                <label className="font-bold text-slate-400">Current Password</label>
                <input 
                  type="password" 
                  required
                  value={pwForm.oldPassword}
                  onChange={e => setPwForm({ ...pwForm, oldPassword: e.target.value })}
                  placeholder="••••••••"
                  className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl px-4 py-3 text-xs focus:outline-none transition-colors"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="font-bold text-slate-400">New Password</label>
                  <input 
                    type="password" 
                    required
                    minLength={6}
                    value={pwForm.newPassword}
                    onChange={e => setPwForm({ ...pwForm, newPassword: e.target.value })}
                    placeholder="••••••••"
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl px-4 py-3 text-xs focus:outline-none transition-colors"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="font-bold text-slate-400">Confirm New Password</label>
                  <input 
                    type="password" 
                    required
                    value={pwForm.confirmPassword}
                    onChange={e => setPwForm({ ...pwForm, confirmPassword: e.target.value })}
                    placeholder="••••••••"
                    className="w-full bg-slate-900 border border-slate-800 focus:border-blue-500 rounded-xl px-4 py-3 text-xs focus:outline-none transition-colors"
                  />
                </div>
              </div>

              <button 
                type="submit" 
                disabled={pwLoading}
                className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-xl transition-all shadow-md shadow-blue-500/20 cursor-pointer"
              >
                {pwLoading ? 'Updating credentials...' : 'Update Password'}
              </button>
            </form>
          </div>

          {/* Token & Appointment History Card */}
          <div className="glass-panel border border-slate-800 rounded-2xl p-6 md:p-8 space-y-6">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800/80">
              <h3 className="text-lg font-bold">Active Tokens & Appointment History</h3>
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-blue-500/10 border border-blue-500/20 text-blue-400">
                {tokens.length} Total
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-800/60 text-[10px] text-slate-400 uppercase tracking-wider">
                    <th className="pb-3 font-semibold">Token No</th>
                    <th className="pb-3 font-semibold">Service/Details</th>
                    <th className="pb-3 font-semibold">Sector</th>
                    <th className="pb-3 font-semibold">Date & Time</th>
                    <th className="pb-3 font-semibold">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-850 text-xs">
                  {tokens.length === 0 ? (
                    <tr>
                      <td colSpan="5" className="text-center py-8 text-slate-500">
                        No active tokens or appointments.
                      </td>
                    </tr>
                  ) : (
                    tokens.map(token => (
                      <tr key={token.id} className="hover:bg-slate-900/20 transition-colors">
                        <td className="py-3.5 font-extrabold text-blue-400">{token.tokenNumber}</td>
                        <td className="py-3.5">
                          <div className="font-semibold text-slate-200">{token.serviceName}</div>
                          <span className={`inline-block mt-0.5 px-2 py-0.25 text-[9px] rounded font-medium border ${
                            token.priority === 'EMERGENCY' 
                              ? 'bg-rose-500/10 border-rose-500/20 text-rose-400' 
                              : 'bg-slate-800 border-slate-700 text-slate-400'
                          }`}>
                            {token.priority}
                          </span>
                        </td>
                        <td className="py-3.5 capitalize text-slate-300">
                          {token.sectorType.toLowerCase()}
                        </td>
                        <td className="py-3.5 text-slate-400 text-[11px]">
                          {new Date(token.createdAt).toLocaleString()}
                        </td>
                        <td className="py-3.5">
                          <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${
                            token.status === 'COMPLETED' ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' :
                            token.status === 'SERVING' ? 'bg-amber-500/10 border-amber-500/20 text-amber-400 animate-pulse' :
                            token.status === 'SKIPPED' ? 'bg-slate-800 border-slate-700 text-slate-500' :
                            token.status === 'CANCELLED' ? 'bg-rose-500/10 border-rose-500/20 text-rose-400' :
                            'bg-blue-500/10 border-blue-500/20 text-blue-400' // PENDING
                          }`}>
                            {token.status}
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Payment Transactions History Card */}
          <div className="glass-panel border border-slate-800 rounded-2xl p-6 md:p-8 space-y-6">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800/80">
              <h3 className="text-lg font-bold">Payment Transactions History</h3>
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                {payments.length} Payments
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-800/60 text-[10px] text-slate-400 uppercase tracking-wider">
                    <th className="pb-3 font-semibold">Transaction ID</th>
                    <th className="pb-3 font-semibold">Sector</th>
                    <th className="pb-3 font-semibold">Amount</th>
                    <th className="pb-3 font-semibold">Method</th>
                    <th className="pb-3 font-semibold">Receipt</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-850 text-xs">
                  {payments.length === 0 ? (
                    <tr>
                      <td colSpan="5" className="text-center py-8 text-slate-500">
                        No transactions found.
                      </td>
                    </tr>
                  ) : (
                    payments.map(payment => (
                      <tr key={payment.id} className="hover:bg-slate-900/20 transition-colors">
                        <td className="py-3.5">
                          <div className="font-mono text-slate-200 font-semibold">{payment.transactionId}</div>
                          <div className="text-[10px] text-slate-500 mt-0.5">{new Date(payment.createdAt).toLocaleString()}</div>
                        </td>
                        <td className="py-3.5">
                          <span className="px-2 py-0.5 rounded bg-slate-900 border border-slate-800 text-[10px] text-slate-400">
                            {payment.sectorType}
                          </span>
                        </td>
                        <td className="py-3.5 font-bold text-slate-200">
                          ₹{payment.amount}
                        </td>
                        <td className="py-3.5 text-slate-400 uppercase text-[10px]">
                          {payment.paymentMethod.replace('_', ' ')}
                        </td>
                        <td className="py-3.5">
                          {payment.paymentStatus === 'PAID' ? (
                            <button
                              onClick={() => window.open(`${API_BASE}/payment/receipt/${payment.id}`, '_blank')}
                              className="px-2.5 py-1.5 bg-blue-600/10 hover:bg-blue-600/25 border border-blue-500/20 hover:border-blue-500/40 text-[10px] text-blue-400 font-bold rounded-lg transition-all flex items-center gap-1.5 cursor-pointer animate-pulse"
                            >
                              <FileText className="w-3 h-3" />
                              Receipt PDF
                            </button>
                          ) : (
                            <span className="text-rose-400 font-semibold text-[10px]">{payment.paymentStatus}</span>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

