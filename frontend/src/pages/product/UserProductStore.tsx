import React, { useEffect, useState } from 'react';
import { 
    Card, Button, Row, Col, Badge, Drawer, List, 
    Typography, message, Descriptions, Modal, Space, Popconfirm 
} from 'antd';
import { 
    ShoppingCartOutlined, EyeOutlined, PlusOutlined, 
    MinusOutlined, DeleteOutlined 
} from '@ant-design/icons';

const { Title, Text } = Typography;

const USER_ID = 111;
const API_BASE_URL = "http://localhost:9090/api";

interface Product {
    id: number;
    name: string;
    description: string;
    price: number;
    imageUrls: string[];
}

const UserProductStore: React.FC = () => {
    const [products, setProducts] = useState<Product[]>([]);
    const [cartData, setCartData] = useState<any>(null);
    const [isCartOpen, setIsCartOpen] = useState(false);
    const [viewProduct, setViewProduct] = useState<Product | null>(null);

    useEffect(() => {
        fetchProducts();
        fetchCart();
    }, []);

    const fetchProducts = async () => {
        try {
            const res = await fetch(`${API_BASE_URL}/products`);
            const data = await res.json();
            setProducts(data.content || data);
        } catch (err) {
            message.error("Failed to fetch products");
        }
    };

    const fetchCart = async () => {
        try {
            const res = await fetch(`${API_BASE_URL}/cart/${USER_ID}`);
            if (res.ok) {
                const data = await res.json();
                setCartData(data);
            }
        } catch (err) {
            console.error("Cart Fetch Error:", err);
        }
    };

    /**
     * @param productId The ID of the product
     * @param amount The value (1 for add/plus, -1 for minus)
     * @param isInitialAdd Boolean to determine if we use POST (new) or PATCH (update)
     */
    const handleCartUpdate = async (productId: number, amount: number, isInitialAdd: boolean = false) => {
        try {
            const url = isInitialAdd 
                ? `${API_BASE_URL}/cart/${USER_ID}/items` 
                : `${API_BASE_URL}/cart/${USER_ID}/items/${productId}`;

            const method = isInitialAdd ? 'POST' : 'PATCH';
            
            // DTO Mapping: 
            // POST expects { productId, quantity }
            // PATCH expects { quantityChange }
            const body = isInitialAdd 
                ? { productId, quantity: amount } 
                : { quantityChange: amount };

            const res = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body),
            });

            if (res.ok) {
                fetchCart();
                if (isInitialAdd) message.success("Added to cart", 0.5);
            } else {
                const errData = await res.json();
                message.error(errData.message || "Update failed");
            }
        } catch (err) {
            message.error("Connection to server failed");
        }
    };

    const handleFullRemove = async (productId: number) => {
        try {
            const res = await fetch(`${API_BASE_URL}/cart/${USER_ID}/items/${productId}`, {
                method: 'DELETE',
            });
            if (res.ok) {
                message.success("Item removed");
                fetchCart();
            }
        } catch (err) {
            message.error("Failed to remove item");
        }
    };

    const handleCheckout = async () => {
    try {
        const res = await fetch(`${API_BASE_URL}/cart/${USER_ID}/checkout`, {
            method: 'POST',
        });

        if (res.ok) {
            message.success("Order Placed Successfully!");
            setIsCartOpen(false); // Close drawer
            fetchCart(); // Refresh cart (will now be empty)
        } else {
            message.error("Checkout failed");
        }
    } catch (err) {
        message.error("Error connecting to server");
    }
};

const getProductImage = (name: string) => {
    const n = name
        .toLowerCase()
        .replace(/[^a-z0-9]/g, ""); // removes spaces, dashes, etc.

    if (n.includes("ultrabook") || n.includes("laptop")) {
        return "https://images.pexels.com/photos/18105/pexels-photo.jpg";
    } else if (n.includes("watch")) {
        return "https://images.pexels.com/photos/277319/pexels-photo-277319.jpeg";
    } else if (n.includes("headphone")) {
        return "https://images.pexels.com/photos/3394650/pexels-photo-3394650.jpeg";
    } else if (n.includes("monitor")) {
        return "https://images.pexels.com/photos/1714208/pexels-photo-1714208.jpeg";
    } else if (n.includes("keyboard")) {
        return "https://images.pexels.com/photos/2115257/pexels-photo-2115257.jpeg";
    } else if (n.includes("jacket")) {
        return "https://images.pexels.com/photos/1124465/pexels-photo-1124465.jpeg";
    } else if (n.includes("tshirt")) {
        return "https://images.pexels.com/photos/1002643/pexels-photo-1002643.jpeg";
    } else if (n.includes("sneaker")) {
        return "https://images.pexels.com/photos/2529148/pexels-photo-2529148.jpeg";
    } else if (n.includes("sunglasses")) {
        return "https://images.pexels.com/photos/46710/pexels-photo-46710.jpeg";
    } else if (n.includes("backpack")) {
        return "https://images.pexels.com/photos/2905238/pexels-photo-2905238.jpeg";
    }

    return "https://via.placeholder.com/400";
};

    return (
        <div style={{ padding: '40px', backgroundColor: '#f0f2f5', minHeight: '100vh' }}>
            {/* HEADER */}
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 30 }}>
                <Title level={2}>Funkart Store</Title>
                <Badge count={cartData?.items?.length || 0} showZero color="#10b981">
                    <Button size="large" icon={<ShoppingCartOutlined />} onClick={() => setIsCartOpen(true)}>
                        My Cart
                    </Button>
                </Badge>
            </div>

            {/* PRODUCT GRID */}
            <Row gutter={[24, 24]}>
                {products.map(p => (
                    <Col xs={24} sm={12} md={8} lg={6} key={p.id}>
                        <Card
                            hoverable
                            cover={<img alt={p.name} src={getProductImage(p.name)} style={{ height: 200, objectFit: 'cover' }} />}
                            actions={[
                                <EyeOutlined key="view" onClick={() => setViewProduct(p)} />,
                                // TRUE passed here triggers the POST API
                                <ShoppingCartOutlined key="add" onClick={() => handleCartUpdate(p.id, 1, true)} style={{ color: '#10b981' }} />
                            ]}
                        >
                            <Card.Meta title={p.name} description={<Text strong style={{ color: '#10b981' }}>${p.price.toFixed(2)}</Text>} />
                        </Card>
                    </Col>
                ))}
            </Row>

            {/* CART DRAWER */}
            <Drawer
                title="Your Basket"
                onClose={() => setIsCartOpen(false)}
                open={isCartOpen}
                width={450}
                footer={
                    <div style={{ padding: '10px 0' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
                            <Text>Total Amount:</Text>
                            <Title level={3} style={{ margin: 0, color: '#10b981' }}>
                                ${cartData?.totalAmount?.toFixed(2) || "0.00"}
                            </Title>
                        </div>
                        <Button type="primary" block size="large" style={{ backgroundColor: '#10b981' }} disabled={!cartData?.items?.length} onClick={handleCheckout}>
                            Proceed to Checkout
                        </Button>
                    </div>
                }
            >
                <List
                    dataSource={cartData?.items || []}
                    renderItem={(item: any) => (
                        <List.Item
                            actions={[
                                <Space>
                                    {/* FALSE passed here triggers the PATCH API */}
                                    <Button size="small" shape="circle" icon={<MinusOutlined />} 
                                        onClick={() => handleCartUpdate(item.productId, -1, false)} 
                                    />
                                    <Text strong>{item.quantity}</Text>
                                    <Button size="small" shape="circle" icon={<PlusOutlined />} 
                                        onClick={() => handleCartUpdate(item.productId, 1, false)} 
                                    />
                                    <Popconfirm title="Delete this item?" onConfirm={() => handleFullRemove(item.productId)}>
                                        <Button size="small" type="text" danger icon={<DeleteOutlined />} />
                                    </Popconfirm>
                                </Space>
                            ]}
                        >
                            <List.Item.Meta title={item.productName} description={`$${item.price.toFixed(2)} each`} />
                            <Text strong>${item.subTotal.toFixed(2)}</Text>
                        </List.Item>
                    )}
                />
            </Drawer>

            {/* QUICK VIEW MODAL */}
            <Modal title="Product Overview" open={!!viewProduct} footer={null} onCancel={() => setViewProduct(null)}>
                {viewProduct && (
                    <div style={{ textAlign: 'center' }}>
                        <img src={getProductImage(viewProduct.name)} style={{ width: '100%', borderRadius: 8, marginBottom: 20 }} />
                        <Descriptions bordered column={1}>
                            <Descriptions.Item label="Price">${viewProduct.price.toFixed(2)}</Descriptions.Item>
                            <Descriptions.Item label="Info">{viewProduct.description}</Descriptions.Item>
                        </Descriptions>
                        <Button type="primary" block size="large" style={{ marginTop: 20, backgroundColor: '#10b981' }} 
                            onClick={() => { handleCartUpdate(viewProduct.id, 1, true); setViewProduct(null); }}>
                            Add to Cart
                        </Button>
                    </div>
                )}
            </Modal>
        </div>
    );
};

export default UserProductStore;