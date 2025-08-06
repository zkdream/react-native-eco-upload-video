/*
 * @Author: 星光 1558471295@qq.com
 * @Date: 2025-08-05 15:27:11
 * @FilePath: \react-native-eco-upload-video\example\src\App.tsx
 * @Description: 
 * Copyright (c) 2025 by 星光, All Rights Reserved. 
 */
import { Text, View, StyleSheet, Platform } from 'react-native';
import { uploadVideoWithUrl } from '../../src/index';
// import { uploadVideoWithUrl } from 'react-native-eco-upload-video';  这种需要修改metro.config.js


export default function App() {
   const uploadURL = "https://upload.dd373.com" 
  return (
    <View style={styles.container}>
      <Text 
      style={{
        width: 100,
        height: 100,
        backgroundColor: 'red',
        color: 'white',
        textAlign: 'center',
        lineHeight: 100,
      }}
      onPress={()=>{
        console.log('====================================');
        console.log(666);
        console.log('====================================');
        let video={
          "originalPath":null,
          "height":1280,
          "width":720,
          "type":"video/mp4",
          "duration":11,
          "fileName":"xzg_64137.mp4",
          "bitrate":1207981,
          "fileSize":1721374,
          "uri":"file:///data/user/0/ecouploadvideo.example/cache/rn_image_picker_lib_temp_8b599ca8-f768-49cd-9336-2cabbf3bf235.mp4"
        }
        uploadVideoWithUrl(uploadURL + '/Api/Upload/UploadFile', '21', video).then((res: string) => {
         console.log('==================555==================');
         console.log(res);
         console.log('===================555=================');
      })
      }}
      >点击</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
