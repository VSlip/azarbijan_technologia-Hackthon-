import express from 'express';
import pg from 'pg';
import dotenv from 'dotenv';

dotenv.config();

const app = express();
app.use(express.json());

const pool = new pg.Pool({
  connectionString: process.env.DATABASE_URL
});

// Test DB connection on startup
(async () => {
  try {
    const result = await pool.query('SELECT NOW()');
    console.log('✅ Database connected at', result.rows[0].now);
  } catch (err) {
    console.error('❌ Database connection failed:', err.message);
  }
})();

// Test route
app.get('/', (req, res) => {
  res.send('Server is running!');
});

// 1️⃣ Register a new device
app.post('/devices', async (req, res) => {
  const { device_type, name } = req.body;
  if (!device_type || !name) {
    return res.status(400).json({ error: 'Missing required fields' });
  }

  try {
    const result = await pool.query(
      `INSERT INTO devices (device_type, name)
       VALUES ($1, $2)
       RETURNING user_id, device_type, name, created_at`,
      [device_type, name]
    );
    res.json(result.rows[0]); // returns the new device row
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Database error' });
  }
});

// 2️⃣ Log screen time data
app.post('/logs', async (req, res) => {
  const { user_id, app_name, start_time, end_time, device_type } = req.body;
  if (!user_id || !app_name || !start_time || !end_time || !device_type) {
    return res.status(400).json({ error: 'Missing required fields' });
  }

  try {
    const result = await pool.query(
      `INSERT INTO screentime_logs (user_id, app_name, start_time, end_time, device_type)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [user_id, app_name, start_time, end_time, device_type]
    );
    res.json(result.rows[0]);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Database error' });
  }
});

app.listen(3000, () => {
  console.log('🚀 Server running on http://localhost:3000');
});