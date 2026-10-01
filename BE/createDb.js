'use strict';
require('dotenv').config();
const { Client } = require('pg');

async function createDatabase() {
  const client = new Client({
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD,
    database: 'postgres' // Connect to the default database
  });

  try {
    await client.connect();
    console.log('Connected to default postgres database.');
    
    const res = await client.query(`SELECT datname FROM pg_catalog.pg_database WHERE datname = 'ttcs_hr_db'`);
    if (res.rowCount === 0) {
      console.log('Database ttcs_hr_db does not exist. Creating...');
      await client.query('CREATE DATABASE ttcs_hr_db');
      console.log('Database ttcs_hr_db created successfully!');
    } else {
      console.log('Database ttcs_hr_db already exists.');
    }
  } catch (err) {
    console.error('Error creating database:', err.message);
  } finally {
    await client.end();
  }
}

createDatabase();
