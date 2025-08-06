/*
 * @Author: 星光 1558471295@qq.com
 * @Date: 2025-08-05 15:27:11
 * @FilePath: \react-native-eco-upload-video\src\NativeEcoUploadVideo.ts
 * @Description: 
 * Copyright (c) 2025 by 星光, All Rights Reserved. 
 */
import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  uploadVideoWithUrl(url: string, fileInfoType: string, videoData: Object): Promise<string>;
  cancelUploadVideoTask(): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('EcoUploadVideo');

