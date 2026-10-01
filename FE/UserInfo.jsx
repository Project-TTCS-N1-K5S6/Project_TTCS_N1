import React from 'react';

export default function UserInfo({ user, onLogout }) {
  // Dữ liệu mock thông tin người dùng (sẽ kết nối API ở task KN-69 sau)
  const currentUser = user || {
    name: "NGUYEN MANH CUONG",
    role: "Admin",
    avatar: "https://ui-avatars.com/api/?name=Nguyen+Manh+Cuong&background=0D8ABC&color=fff"
  };

  return (
    <div className="flex items-center gap-3 p-3 rounded-lg bg-slate-100 hover:bg-slate-200 transition border border-slate-200">
      <img 
        src={currentUser.avatar} 
        alt={currentUser.name} 
        className="w-10 h-10 rounded-full border-2 border-blue-500 object-cover"
      />
      <div className="flex flex-col text-sm">
        <span className="font-semibold text-slate-800">{currentUser.name}</span>
        <span className="text-xs text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full font-medium w-fit border border-blue-200">
          {currentUser.role}
        </span>
      </div>
      <button 
        onClick={onLogout}
        className="ml-auto text-xs text-red-600 hover:text-red-800 font-medium px-2.5 py-1.5 rounded hover:bg-red-50 border border-transparent hover:border-red-200 transition"
      >
        Đăng xuất
      </button>
    </div> 
  );
} // test KN-67