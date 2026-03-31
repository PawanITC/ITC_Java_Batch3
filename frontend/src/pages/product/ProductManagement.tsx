import React, { useEffect, useState } from 'react';
import { 
    Table, Button, Space, Popconfirm, Modal, Input, 
    InputNumber, Form, message, Card, Descriptions, 
    Tooltip, Row, Col, Badge, Typography, Drawer, List, Avatar 
} from 'antd';
import { 
    EyeOutlined, EditOutlined, DeleteOutlined, 
    ShoppingCartOutlined, PlusOutlined, MinusOutlined 
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';

const { Title, Text } = Typography;

interface Product {
    id: number; 
    name: string;
    description: string;
    price: number;
   // imageUrl?: string;
}

const BASE_URL = "http://localhost:9090/api/products";

const ProductManagement: React.FC = () => {
    // --- CHANGE THIS TO 'admin' or 'user' TO SWITCH VIEWS ---
    const [userRole] = useState<'admin' | 'user'>('user'); 
    
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(false);
    
    // --- CART STATE ---
    const [cart, setCart] = useState<{ [key: number]: number }>({});
    const [isCartOpen, setIsCartOpen] = useState(false);
    
    // --- MODAL & FORM STATES ---
    const [isFormModalOpen, setIsFormModalOpen] = useState(false);
    const [isViewModalOpen, setIsViewModalOpen] = useState(false);
    const [viewProduct, setViewProduct] = useState<Product | null>(null);
    const [isEditing, setIsEditing] = useState(false);
    const [form] = Form.useForm();

    useEffect(() => {
        fetchProducts();
    }, []);

    // --- API: FETCH ALL ---
    const fetchProducts = async () => {
        setLoading(true);
        try {
            const res = await fetch(BASE_URL);
            const data = await res.json();
            setProducts(data);
        } catch (err) {
            message.error("Failed to fetch products");
        } finally {
            setLoading(false);
        }
    };

    // --- API: DELETE ---
    const handleDelete = async (id: number) => {
        try {
            const res = await fetch(`${BASE_URL}/${id}`, { method: 'DELETE' });
            if (res.ok) {
                message.success("Product removed");
                fetchProducts();
            }
        } catch (err) {
            message.error("Delete failed");
        }
    };

    // --- API: CREATE / UPDATE ---
    const handleFinish = async (values: Product) => {
        const method = isEditing ? 'PUT' : 'POST';
        const url = isEditing ? `${BASE_URL}/${form.getFieldValue('id')}` : BASE_URL;

        try {
            const res = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(values),
            });

            if (res.ok) {
                message.success(`Product ${isEditing ? 'updated' : 'created'}`);
                setIsFormModalOpen(false);
                fetchProducts();
            }
        } catch (err) {
            message.error("Operation failed");
        }
    };

    // --- CART LOGIC ---
    const addToCart = (id: number) => {
        setCart(prev => ({ ...prev, [id]: (prev[id] || 0) + 1 }));
        message.success("Added to cart", 0.5);
    };

    const removeFromCart = (id: number) => {
        setCart(prev => {
            const newCart = { ...prev };
            if (newCart[id] > 1) newCart[id] -= 1;
            else delete newCart[id];
            return newCart;
        });
    };

    const clearFromCart = (id: number) => {
        setCart(prev => {
            const newCart = { ...prev };
            delete newCart[id];
            return newCart;
        });
    };

    const totalCartItems = Object.values(cart).reduce((sum, count) => sum + count, 0);
    
    const calculateTotal = () => {
        return Object.keys(cart).reduce((total, id) => {
            const product = products.find(p => p.id === parseInt(id));
            return total + (product ? product.price * cart[parseInt(id)] : 0);
        }, 0);
    };

    // --- ADMIN TABLE COLUMNS ---
    const columns: ColumnsType<Product> = [
        { title: 'Name', dataIndex: 'name', key: 'name', sorter: (a, b) => a.name.localeCompare(b.name) },
        { title: 'Price', dataIndex: 'price', key: 'price', render: (p) => `$${p.toFixed(2)}`, sorter: (a, b) => a.price - b.price },
        {
            title: 'Actions',
            key: 'actions',
            render: (_, record) => (
                <Space size="middle">
                    <Tooltip title="View"><Button type="text" icon={<EyeOutlined style={{ color: '#1890ff' }} />} onClick={() => { setViewProduct(record); setIsViewModalOpen(true); }} /></Tooltip>
                    <Tooltip title="Edit"><Button type="text" icon={<EditOutlined style={{ color: '#10b981' }} />} onClick={() => { setIsEditing(true); form.setFieldsValue(record); setIsFormModalOpen(true); }} /></Tooltip>
                    <Tooltip title="Delete">
                        <Popconfirm title="Delete this product?" onConfirm={() => handleDelete(record.id)}>
                            <Button type="text" danger icon={<DeleteOutlined />} />
                        </Popconfirm>
                    </Tooltip>
                </Space>
            ),
        },
    ];

    return (
        <div style={{ padding: '40px', backgroundColor: '#f0f2f5', minHeight: '100vh' }}>
            {/* HEADER */}
            <div style={{ marginBottom: 30, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title level={2}>{userRole === 'admin' ? 'Inventory Manager' : 'Storefront'}</Title>
                
                {userRole === 'admin' ? (
                    <Button 
                        type="primary" 
                        size="large" 
                        icon={<PlusOutlined />}
                        onClick={() => { setIsEditing(false); form.resetFields(); setIsFormModalOpen(true); }} 
                        style={{ backgroundColor: '#10b981', border: 'none' }}
                    >
                        Create Product
                    </Button>
                ) : (
                    <Badge count={totalCartItems} showZero color="#10b981">
                        <Button type="default" size="large" icon={<ShoppingCartOutlined />} onClick={() => setIsCartOpen(true)}>
                            My Cart
                        </Button>
                    </Badge>
                )}
            </div>

            {/* MAIN CONTENT */}
            {userRole === 'admin' ? (
                <div style={{ background: 'white', padding: '20px', borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}>
                    <Table columns={columns} dataSource={products} rowKey="id" loading={loading} pagination={{ pageSize: 5 }} />
                </div>
            ) : (
                <Row gutter={[24, 24]}>
                    {products.map(product => (
                        <Col xs={24} sm={12} md={8} lg={6} key={product.id}>
                            <Card
                                hoverable
                               // cover={<img alt={product.name} src={product.imageUrl || 'https://via.placeholder.com/250'} style={{ height: 200, objectFit: 'cover' }} />}
                                actions={[
                                    <EyeOutlined key="view" onClick={() => { setViewProduct(product); setIsViewModalOpen(true); }} />,
                                    <ShoppingCartOutlined key="add" onClick={() => addToCart(product.id)} style={{ color: '#10b981' }} />
                                ]}
                            >
                                <Card.Meta 
                                    title={product.name} 
                                    description={<Text strong style={{ color: '#10b981', fontSize: '16px' }}>${product.price.toFixed(2)}</Text>} 
                                />
                            </Card>
                        </Col>
                    ))}
                </Row>
            )}

            {/* --- CART DRAWER --- */}
            <Drawer
                title="Your Shopping Basket"
                onClose={() => setIsCartOpen(false)}
                open={isCartOpen}
                width={450}
                footer={
                    <div style={{ padding: '10px 0' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
                            <Text>Grand Total:</Text>
                            <Title level={3} style={{ margin: 0, color: '#10b981' }}>${calculateTotal().toFixed(2)}</Title>
                        </div>
                        <Button type="primary" block size="large" style={{ backgroundColor: '#10b981' }} disabled={totalCartItems === 0}>
                            Proceed to Checkout
                        </Button>
                    </div>
                }
            >
                <List
                    itemLayout="horizontal"
                    dataSource={Object.keys(cart)}
                    renderItem={(idStr) => {
                        const id = parseInt(idStr);
                        const item = products.find(p => p.id === id);
                        if (!item) return null;
                        return (
                            <List.Item
                                actions={[
                                    <Space>
                                        <Button size="small" shape="circle" icon={<MinusOutlined />} onClick={() => removeFromCart(id)} />
                                        <Text strong>{cart[id]}</Text>
                                        <Button size="small" shape="circle" icon={<PlusOutlined />} onClick={() => addToCart(id)} />
                                        <Popconfirm title="Remove item?" onConfirm={() => clearFromCart(id)}>
                                            <Button size="small" type="text" danger icon={<DeleteOutlined />} />
                                        </Popconfirm>
                                    </Space>
                                ]}
                            >
                                <List.Item.Meta
                                    //avatar={<Avatar src={item.imageUrl} shape="square" size="large" />}
                                    title={item.name}
                                    description={`$${item.price.toFixed(2)} each`}
                                />
                                <div><Text strong>${(item.price * cart[id]).toFixed(2)}</Text></div>
                            </List.Item>
                        );
                    }}
                />
            </Drawer>

            {/* --- VIEW MODAL --- */}
            <Modal title="Product Overview" open={isViewModalOpen} footer={null} onCancel={() => setIsViewModalOpen(false)} width={600}>
                {viewProduct && (
                    <div style={{ textAlign: 'center' }}>
                        {/* <img src={viewProduct.imageUrl} style={{ maxWidth: '100%', borderRadius: '8px', marginBottom: '20px' }} /> */}
                        <Descriptions title={viewProduct.name} bordered column={1}>
                            <Descriptions.Item label="Price"><Text strong style={{ color: '#10b981' }}>${viewProduct.price.toFixed(2)}</Text></Descriptions.Item>
                            <Descriptions.Item label="About">{viewProduct.description}</Descriptions.Item>
                        </Descriptions>
                    </div>
                )}
            </Modal>

            {/* --- CREATE / EDIT MODAL --- */}
            <Modal
                title={isEditing ? "Modify Product" : "Add New Product"}
                open={isFormModalOpen}
                onOk={() => form.submit()}
                onCancel={() => setIsFormModalOpen(false)}
                okButtonProps={{ style: { backgroundColor: '#10b981' } }}
            >
                <Form form={form} layout="vertical" onFinish={handleFinish}>
                    <Form.Item name="id" hidden><Input /></Form.Item>
                    <Form.Item name="name" label="Product Name" rules={[{ required: true }]}><Input /></Form.Item>
                    <Form.Item name="description" label="Description" rules={[{ required: true }]}><Input.TextArea rows={3} /></Form.Item>
                    <Form.Item name="price" label="Price" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} min={0} precision={2} /></Form.Item>
                    {/* <Form.Item name="imageUrl" label="Image URL"><Input placeholder="https://..." /></Form.Item> */}
                </Form>
            </Modal>
        </div>
    );
};

export default ProductManagement;