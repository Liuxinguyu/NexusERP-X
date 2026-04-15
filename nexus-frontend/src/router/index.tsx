import { createBrowserRouter, Navigate } from 'react-router-dom'
import MainLayout from '../layouts/MainLayout'
import LoginPage from '../pages/LoginPage'
import { ProtectedRoute } from './ProtectedRoute'
import DashboardPage from '../pages/DashboardPage'
import SaleOrderPage from '../pages/erp/SaleOrderPage'

export const router = createBrowserRouter(
  [
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        path: '/',
        element: <MainLayout />,
        children: [
          { index: true, element: <DashboardPage /> },
          { path: '/erp/sale-order', element: <SaleOrderPage /> },
        ],
      },
    ],
  },
  {
    path: '*',
    element: <Navigate to="/" replace />,
  },
  ],
  {
    future: {
      v7_startTransition: true,
    },
  },
)
