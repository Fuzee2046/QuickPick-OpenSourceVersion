module.exports = {
  plugins: {
    'postcss-px-to-viewport-8-plugin': {
      viewportWidth: 375, // 设计稿宽度
      unitPrecision: 5,
      viewportUnit: 'vw',
      // Merchant pages keep the 375px mobile conversion. Desktop-capable
      // public/admin pages and Element Plus must keep their responsive pixels.
      selectorBlackList: ['.admin-', '.el-', '.login-mode', '.header-logo'],
      // 管理端是桌面端固定像素布局，不能按商户移动端375px设计稿转换为vw。
      exclude: [
        /node_modules[\\/]element-plus/,
        /src[\\/]views[\\/]admin[\\/]/,
        /src[\\/]views[\\/]Login\.vue/,
        /src[\\/]views[\\/]Portfolio\.vue/,
      ],
      minPixelValue: 1,
      mediaQuery: false,
    },
  },
};
