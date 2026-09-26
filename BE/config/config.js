module = module.exports = {
  PORT: process.env.PORT || 5000,
  JWT_SECRET: process.env.JWT_SECRET || 'ttcs_recruitment_super_secret_key_2026',
  JWT_EXPIRES_IN: '24h'
};
