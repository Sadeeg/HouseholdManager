/**
 * Dev-Proxy: leitet /api an das Backend weiter.
 *
 * Das Ziel ist überschreibbar (BACKEND_URL), damit der Dev-Server neben
 * anderen Diensten auf einem freien Port laufen kann:
 *   BACKEND_URL=http://localhost:8081 npx ng serve --port 4300
 */
const target = process.env['BACKEND_URL'] ?? 'http://localhost:8080';

module.exports = {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
  },
};
