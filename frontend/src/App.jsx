import React from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import AppLayout from './layouts/AppLayout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import DevicesPage from './pages/DevicesPage'
import DeviceDetailsPage from './pages/DeviceDetailsPage'
import GalleryPage from './pages/GalleryPage'
import ImageViewerPage from './pages/ImageViewerPage'
import VideoViewerPage from './pages/VideoViewerPage'
import ActivityLogsPage from './pages/ActivityLogsPage'
import SettingsPage from './pages/SettingsPage'
import ProfilePage from './pages/ProfilePage'
import { useAuth } from './context/AuthContext'

export default function App() {
  const { isAuthenticated } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/devices" element={<DevicesPage />} />
          <Route path="/devices/:id" element={<DeviceDetailsPage />} />
          <Route path="/gallery" element={<GalleryPage />} />
          <Route path="/gallery/image/:deviceId/:mediaId" element={<ImageViewerPage />} />
          <Route path="/gallery/video/:deviceId/:mediaId" element={<VideoViewerPage />} />
          <Route path="/activity" element={<ActivityLogsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
