import EcoUploadVideo from './NativeEcoUploadVideo';


export function uploadVideoWithUrl(url: string, fileInfoType: string, videoData: Object): Promise<string> {
  return EcoUploadVideo.uploadVideoWithUrl(url, fileInfoType, videoData);
}

export function cancelUploadVideoTask(): void {
  return EcoUploadVideo.cancelUploadVideoTask();
}
