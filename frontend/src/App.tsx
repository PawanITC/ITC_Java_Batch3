import { BrowserRouter, Routes, Route } from "react-router-dom";
import Layout from "./components/Layout";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import OAuthCallback from "./pages/OAuthCallback";
import { AuthProvider } from "./context/AuthProvider";
// import ProductManagement from "./pages/product/ProductManagement";

// import UserProductStore from "./pages/product/UserProductStore";
// import AdminProductManagement from "./pages/product/AdminProductManagement";

function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<Layout />}>
                        <Route index element={<Home />} />
                        <Route path="login" element={<Login />} />
                        <Route path="signup" element={<Signup />} />
                        <Route path="oauth-success" element={<OAuthCallback />} />
                        {/* <Route path="products" element={<ProductManagement />} /> */}

                        {/* USER STOREFRONT: List, View, and Cart logic */}
                        {/* <Route path="products" element={<UserProductStore />} /> */}
                        
                        {/* ADMIN DASHBOARD: Create, Update, Delete logic */}
                        {/* <Route path="admin-products" element={<AdminProductManagement />} /> */}
                    </Route>
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}

export default App;