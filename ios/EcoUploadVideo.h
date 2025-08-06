/*
 * @Author: 星光 1558471295@qq.com
 * @Date: 2025-08-05 15:27:11
 * @FilePath: \react-native-eco-upload-video\ios\EcoUploadVideo.h
 * @Description: 
 * Copyright (c) 2025 by 星光, All Rights Reserved. 
 */
#import <EcoUploadVideoSpec/EcoUploadVideoSpec.h>
#import <Foundation/Foundation.h>
#import <AFNetworking/AFNetworking.h>

@interface EcoUploadVideo : NSObject <NativeEcoUploadVideoSpec>
@property(nonatomic, strong) NSMutableArray *allSessionTask;

- (NSString *)stringWithUUID;
@end
