import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Popconfirm, Modal, Input, InputNumber, Form, message, Card, Descriptions, Tooltip } from 'antd';
import { EyeOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'; // Import Icons
import type { ColumnsType } from 'antd/es/table';
import '../../styles/ProductPage.css';

interface Product {
    id?: number;
    name: string;
    description: string;
    price: number;
    imageUrl?: string;
}

const BASE_URL = "http://localhost:9090/api/products";

const ProductManagement: React.FC = () => {
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(false);
    
    const [isFormModalOpen, setIsFormModalOpen] = useState(false);
    const [isViewModalOpen, setIsViewModalOpen] = useState(false);
    const [viewProduct, setViewProduct] = useState<Product | null>(null);
    const [isEditing, setIsEditing] = useState(false);
    
    const [form] = Form.useForm();

    useEffect(() => {
        fetchProducts();
    }, []);

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

    const handleViewSingle = async (id: number) => {
        try {
            const res = await fetch(`${BASE_URL}/${id}`);
            const data = await res.json();
            setViewProduct(data);
            setIsViewModalOpen(true);
        } catch (err) {
            message.error("Could not load product details");
        }
    };

    const handleDelete = async (id: number) => {
        const res = await fetch(`${BASE_URL}/${id}`, { method: 'DELETE' });
        if (res.ok) {
            message.success("Product removed");
            fetchProducts();
        }
    };

    const handleFinish = async (values: Product) => {
        const method = isEditing ? 'PUT' : 'POST';
        const url = isEditing ? `${BASE_URL}/${form.getFieldValue('id')}` : BASE_URL;

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
    };

    const columns: ColumnsType<Product> = [
        { title: 'Name', dataIndex: 'name', key: 'name', sorter: (a, b) => a.name.localeCompare(b.name) },
        { title: 'Price', dataIndex: 'price', key: 'price', render: (p) => `$${p.toFixed(2)}`, sorter: (a, b) => a.price - b.price },
        {
            title: 'Actions',
            key: 'actions',
            render: (_, record) => (
                <Space size="middle">
                    {/* VIEW ICON */}
                    <Tooltip title="View">
                        <Button 
                            type="text" 
                            icon={<EyeOutlined style={{ color: '#1890ff' }} />} 
                            onClick={() => handleViewSingle(record.id!)} 
                        />
                    </Tooltip>

                    {/* EDIT ICON */}
                    <Tooltip title="Edit">
                        <Button 
                            type="text" 
                            icon={<EditOutlined style={{ color: '#10b981' }} />} 
                            onClick={() => {
                                setIsEditing(true);
                                form.setFieldsValue(record);
                                setIsFormModalOpen(true);
                            }} 
                        />
                    </Tooltip>

                    {/* DELETE ICON */}
                    <Tooltip title="Delete">
                        <Popconfirm title="Are you sure you want to delete?" onConfirm={() => handleDelete(record.id!)}>
                            <Button 
                                type="text" 
                                danger 
                                icon={<DeleteOutlined />} 
                            />
                        </Popconfirm>
                    </Tooltip>
                </Space>
            ),
        },
    ];

    return (
        <div className="product-page-container">
            <div className="product-header" style={{ marginBottom: 20, display: 'flex', justifyContent: 'space-between' }}>
                <h2>Product Inventory</h2>
                <Button type="primary" size="large" onClick={() => {
                    setIsEditing(false);
                    form.resetFields();
                    setIsFormModalOpen(true);
                }} style={{ backgroundColor: '#10b981', border: 'none' }}>
                    + Create Product
                </Button>
            </div>

            <div style={{ background: 'white', padding: '20px', borderRadius: '10px', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}>
                <Table columns={columns} dataSource={products} rowKey="id" loading={loading} pagination={{ pageSize: 5 }} />
            </div>

            {/* --- VIEW CARD MODAL --- */}
            <Modal
                title="Product Details"
                open={isViewModalOpen}
                footer={null}
                onCancel={() => setIsViewModalOpen(false)}
                width={500}
            >
                {viewProduct && (
                    <Card
                        cover={viewProduct.imageUrl && <img alt={viewProduct.name} src={viewProduct.imageUrl} style={{ maxHeight: '300px', objectFit: 'contain', padding: '10px' }} />}
                    >
                        <Descriptions title={viewProduct.name} column={1} bordered>
                            <Descriptions.Item label="Price">
                                <span style={{ color: '#10b981', fontWeight: 'bold', fontSize: '18px' }}>
                                    ${viewProduct.price.toFixed(2)}
                                </span>
                            </Descriptions.Item>
                            <Descriptions.Item label="Description">
                                {viewProduct.description}
                            </Descriptions.Item>
                        </Descriptions>
                    </Card>
                )}
            </Modal>

            {/* --- CREATE/EDIT FORM MODAL --- */}
            <Modal
                title={isEditing ? "Edit Product" : "New Product"}
                open={isFormModalOpen}
                onOk={() => form.submit()}
                onCancel={() => setIsFormModalOpen(false)}
                okButtonProps={{ style: { backgroundColor: '#10b981' } }}
            >
                <Form form={form} layout="vertical" onFinish={handleFinish}>
                    <Form.Item name="id" hidden><Input /></Form.Item>
                    <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
                    <Form.Item name="description" label="Description" rules={[{ required: true }]}><Input.TextArea /></Form.Item>
                    <Form.Item name="price" label="Price" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default ProductManagement;