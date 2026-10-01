import app from './app.js';

const PORT = process.env.PORT || 5000;

app.listen(PORT, () => {
  console.log(`🚀 BE Server running on http://localhost:${PORT}`);
  console.log(`📋 Standardized Error Exception Handling Active (User Story KN-17)`);
});
