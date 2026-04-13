import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Popconfirm, Modal, Input, InputNumber, Form, message, Typography, Tooltip } from 'antd';
import { EyeOutlined, EditOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';

const { Title } = Typography;
const ADMIN_BASE_URL = "http://localhost:9090/api/admin/products";
const PUBLIC_BASE_URL = "http://localhost:9090/api/products";

interface Product {
    id: number; 
    name: string;
    description: string;
    price: number;
    categoryId?: number;
}

const AdminProductManagement: React.FC = () => {
    const [products, setProducts] = useState<Product[]>([]);
    const [loading, setLoading] = useState(false);
    const [isFormModalOpen, setIsFormModalOpen] = useState(false);
    const [isEditing, setIsEditing] = useState(false);
    const [form] = Form.useForm();

    useEffect(() => { fetchProducts(); }, []);

    const fetchProducts = async () => {
        setLoading(true);
        try {
            const res = await fetch(PUBLIC_BASE_URL);
            const data = await res.json();
            // Handle Paginated Response from backend
            setProducts(data.content || data); 
        } catch (err) {
            message.error("Failed to fetch products");
        } finally { setLoading(false); }
    };

    const handleDelete = async (id: number) => {
        try {
            const res = await fetch(`${ADMIN_BASE_URL}/${id}`, { method: 'DELETE' });
            if (res.ok) {
                message.success("Product removed");
                fetchProducts();
            }
        } catch (err) { message.error("Delete failed"); }
    };

    const handleFinish = async (values: any) => {
        const method = isEditing ? 'PUT' : 'POST';
        const url = isEditing ? `${ADMIN_BASE_URL}/${values.id}` : ADMIN_BASE_URL;

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
        } catch (err) { message.error("Operation failed"); }
    };

    const columns: ColumnsType<Product> = [
        { title: 'ID', dataIndex: 'id', key: 'id' },
        { title: 'Name', dataIndex: 'name', key: 'name' },
        { title: 'Price', dataIndex: 'price', key: 'price', render: (p) => `$${p.toFixed(2)}` },
        {
            title: 'Actions',
            key: 'actions',
            render: (_, record) => (
                <Space size="middle">
                    <Button type="text" icon={<EditOutlined style={{ color: '#10b981' }} />} onClick={() => { setIsEditing(true); form.setFieldsValue(record); setIsFormModalOpen(true); }} />
                    <Popconfirm title="Delete this product?" onConfirm={() => handleDelete(record.id)}>
                        <Button type="text" danger icon={<DeleteOutlined />} />
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    return (
        <div style={{ padding: '40px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
                <Title level={2}>Inventory Manager (Admin)</Title>
                <Button type="primary" icon={<PlusOutlined />} onClick={() => { setIsEditing(false); form.resetFields(); setIsFormModalOpen(true); }}>
                    Create Product
                </Button>
            </div>
            <Table columns={columns} dataSource={products} rowKey="id" loading={loading} pagination={{ pageSize: 5 }} />
            
            <Modal title={isEditing ? "Edit Product" : "New Product"} open={isFormModalOpen} onOk={() => form.submit()} onCancel={() => setIsFormModalOpen(false)}>
                <Form form={form} layout="vertical" onFinish={handleFinish}>
                    <Form.Item name="id" hidden><Input /></Form.Item>
                    <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
                    <Form.Item name="description" label="Description" rules={[{ required: true }]}><Input.TextArea /></Form.Item>
                    <Form.Item name="price" label="Price" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} /></Form.Item>
                    <Form.Item name="categoryId" label="Category" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} /></Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default AdminProductManagement;
// import React, { useEffect, useState } from 'react';
// import { Table, Button, Space, Popconfirm, Modal, Input, InputNumber, Form, message, Typography, Tooltip } from 'antd';
// import { EyeOutlined, EditOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons';
// import type { ColumnsType } from 'antd/es/table';

// const { Title } = Typography;
// const ADMIN_BASE_URL = "http://localhost:9090/api/admin/products";
// const PUBLIC_BASE_URL = "http://localhost:9090/api/products";

// interface Product {
//     id: number; 
//     name: string;
//     description: string;
//     price: number;
//     categoryId?: number;
// }

// const AdminProductManagement: React.FC = () => {
//     const [products, setProducts] = useState<Product[]>([]);
//     const [loading, setLoading] = useState(false);
//     const [isFormModalOpen, setIsFormModalOpen] = useState(false);
//     const [isEditing, setIsEditing] = useState(false);
//     const [form] = Form.useForm();

//     useEffect(() => { fetchProducts(); }, []);

//     const fetchProducts = async () => {
//         setLoading(true);
//         try {
//             const res = await fetch(PUBLIC_BASE_URL);
//             const data = await res.json();
//             // Handle Paginated Response from backend
//             setProducts(data.content || data); 
//         } catch (err) {
//             message.error("Failed to fetch products");
//         } finally { setLoading(false); }
//     };

//     const handleDelete = async (id: number) => {
//         try {
//             const res = await fetch(`${ADMIN_BASE_URL}/${id}`, { method: 'DELETE' });
//             if (res.ok) {
//                 message.success("Product removed");
//                 fetchProducts();
//             }
//         } catch (err) { message.error("Delete failed"); }
//     };

//     const handleFinish = async (values: any) => {
//         const method = isEditing ? 'PUT' : 'POST';
//         const url = isEditing ? `${ADMIN_BASE_URL}/${values.id}` : ADMIN_BASE_URL;

//         try {
//             const res = await fetch(url, {
//                 method,
//                 headers: { 'Content-Type': 'application/json' },
//                 body: JSON.stringify(values),
//             });

//             if (res.ok) {
//                 message.success(`Product ${isEditing ? 'updated' : 'created'}`);
//                 setIsFormModalOpen(false);
//                 fetchProducts();
//             }
//         } catch (err) { message.error("Operation failed"); }
//     };

//     const columns: ColumnsType<Product> = [
//         { title: 'ID', dataIndex: 'id', key: 'id' },
//         { title: 'Name', dataIndex: 'name', key: 'name' },
//         { title: 'Price', dataIndex: 'price', key: 'price', render: (p) => `$${p.toFixed(2)}` },
//         {
//             title: 'Actions',
//             key: 'actions',
//             render: (_, record) => (
//                 <Space size="middle">
//                     <Button type="text" icon={<EditOutlined style={{ color: '#10b981' }} />} onClick={() => { setIsEditing(true); form.setFieldsValue(record); setIsFormModalOpen(true); }} />
//                     <Popconfirm title="Delete this product?" onConfirm={() => handleDelete(record.id)}>
//                         <Button type="text" danger icon={<DeleteOutlined />} />
//                     </Popconfirm>
//                 </Space>
//             ),
//         },
//     ];

//     return (
//         <div style={{ padding: '40px' }}>
//             <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
//                 <Title level={2}>Inventory Manager (Admin)</Title>
//                 <Button type="primary" icon={<PlusOutlined />} onClick={() => { setIsEditing(false); form.resetFields(); setIsFormModalOpen(true); }}>
//                     Create Product
//                 </Button>
//             </div>
//             <Table columns={columns} dataSource={products} rowKey="id" loading={loading} pagination={{ pageSize: 5 }} />
            
//             <Modal title={isEditing ? "Edit Product" : "New Product"} open={isFormModalOpen} onOk={() => form.submit()} onCancel={() => setIsFormModalOpen(false)}>
//                 <Form form={form} layout="vertical" onFinish={handleFinish}>
//                     <Form.Item name="id" hidden><Input /></Form.Item>
//                     <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
//                     <Form.Item name="description" label="Description" rules={[{ required: true }]}><Input.TextArea /></Form.Item>
//                     <Form.Item name="price" label="Price" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} /></Form.Item>
//                     <Form.Item name="categoryId" label="Category" rules={[{ required: true }]}><InputNumber style={{ width: '100%' }} /></Form.Item>
//                 </Form>
//             </Modal>
//         </div>
//     );
// };

// export default AdminProductManagement;
