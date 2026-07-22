import { Navigate, Route, Routes } from "react-router-dom";
import ProtectedRoute from "./components/ProtectedRoute";
import Layout from "./components/Layout";
import Landing from "./pages/Landing";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Documents from "./pages/Documents";
import Query from "./pages/Query";
import Reference from "./pages/Reference";
import Settings from "./pages/Settings";

export default function App() {
  return (
    <Routes>
      {/* Public marketing site */}
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />

      {/* Authenticated product, mounted under /app */}
      <Route
        path="/app"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Dashboard />} />
        <Route path="documents" element={<Documents />} />
        <Route path="query" element={<Query />} />
        <Route path="reference" element={<Reference />} />
        <Route path="settings" element={<Settings />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
