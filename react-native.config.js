module.exports = {
  project: {
    ios: {
      automaticPodsInstallation: true,
    },
  },
  dependencies: {
    'react-native-eco-upload-video': {
      root: __dirname,
      platforms: {
        ios: {},
        android: {},
      },
    },
  },
}; 