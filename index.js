import express from 'express';
import pg from 'pg';
import dotenv from 'dotenv';
import cors from 'cors';

dotenv.config();

const app = express();
app.use(express.json());

const pool = new pg.Pool({
  connectionString: process.env.DATABASE_URL
});

app.use(cors({ origin: '*' }));

// 🔍 DB identity check — runs once on startup
(async () => {
  try {
    const info = await pool.query(`
      SELECT current_database() AS database_name,
             current_user AS connected_user,
             inet_server_addr() AS server_ip,
             inet_server_port() AS server_port
    `);
    console.log('📡 Connected to DB:', info.rows[0]);
  } catch (err) {
    console.error('❌ Could not fetch DB info:', err.message);
  }
})();

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

// 🩺 Health check route
app.get('/health', async (req, res) => {
  try {
    // Optional: check DB connectivity too
    const dbCheck = await pool.query('SELECT NOW() AS now');
    res.json({
      status: 'ok',
      server_time: new Date().toISOString(),
      database_time: dbCheck.rows[0].now
    });
  } catch (err) {
    res.status(500).json({
      status: 'error',
      message: 'Database connection failed',
      error: err.message
    });
  }
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
    console.error('❌ /devices DB error:', err); // 🔍 Detailed error log
    res.status(500).json({ error: err.message }); // return actual DB error message
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
    console.error('❌ /logs DB error:', err); // 🔍 Detailed error log
    res.status(500).json({ error: err.message }); // return actual DB error message
  }
});

// 3️⃣ Fetch aggregated screen time data
app.get('/logs', async (req, res) => {
    try {
        const result = await pool.query(
            `SELECT * FROM screentime_logs ORDER BY start_time DESC`
        );
        res.json(result.rows);
    } catch (err) {
        console.error('❌ /logs fetch error:', err);
        res.status(500).json({ error: err.message });
    }
});

app.listen(3000, '0.0.0.0', () => {
  console.log('🚀 Server running on http://localhost:3000');
});