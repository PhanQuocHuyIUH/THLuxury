const API_URL = 'http://localhost:8080/api';
const ADMIN_EMAIL = 'admin@thluxury.local';
const ADMIN_PASS = 'Demo@123';

async function run() {
  console.log('Logging in as admin...');
  const loginRes = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASS })
  });

  if (!loginRes.ok) {
    console.error('Login failed:', await loginRes.text());
    return;
  }

  const { accessToken } = await loginRes.json();
  console.log('Login successful. Fetching products...');

  const productsRes = await fetch(`${API_URL}/products?size=1000`);
  if (!productsRes.ok) {
    console.error('Failed to fetch products:', await productsRes.text());
    return;
  }

  const productsData = await productsRes.json();
  const products = productsData.content || [];
  
  const invalidProducts = products.filter(p => p.id.startsWith('0158ed6f'));
  
  if (invalidProducts.length === 0) {
    console.log('No invalid demo products found.');
    return;
  }

  console.log(`Found ${invalidProducts.length} invalid demo products. Deleting via API...`);

  for (const product of invalidProducts) {
    console.log(`Archiving/Deleting product ${product.id} - ${product.tenSp}...`);
    const delRes = await fetch(`${API_URL}/products/${product.id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });

    if (delRes.ok) {
      console.log(`✅ Success: ${product.id}`);
    } else {
      console.error(`❌ Failed to delete ${product.id}:`, await delRes.text());
    }
  }

  console.log('Cleanup complete!');
}

run().catch(console.error);