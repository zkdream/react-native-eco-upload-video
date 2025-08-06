#import "EcoUploadVideo.h"

@implementation 
- (instancetype)init {
    self = [super init];
    if (self) {
        _allSessionTask = [NSMutableArray array];
    }
    return self;
}

- (void)dealloc {
    _allSessionTask = nil;
}

RCT_EXPORT_MODULE()

- (void)uploadVideoWithUrl:(NSString *)url fileInfoType:(NSString *)fileInfoType videoData:(NSDictionary *)videoData resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject
{
    // 参数验证
    if (!url || url.length == 0) {
        reject(@"INVALID_PARAM", @"URL cannot be empty", nil);
        return;
    }
    if (!videoData || !videoData[@"uri"]) {
        reject(@"INVALID_PARAM", @"Video data or URI is missing", nil);
        return;
    }

    dispatch_async(dispatch_get_main_queue(), ^{ 
        NSError *dataError;
        NSURL *fileURL = [NSURL URLWithString:videoData[@"uri"]];
        NSData *data = [NSData dataWithContentsOfURL:fileURL options:NSDataReadingMappedAlways error:&dataError];

        if (dataError) {
            // 确保在主线程调用reject
            dispatch_async(dispatch_get_main_queue(), ^{ 
                reject(@"DATA_ERROR", dataError.localizedDescription, dataError);
            });
            return;
        }

        double truck = 1024 * 1024 * 2.0; // 2M
        NSInteger count = ceil(data.length / truck);
        NSString *name = [[self stringWithUUID] stringByAppendingString:@".mp4"];

        dispatch_async(dispatch_get_global_queue(0, 0), ^{ 
            // 创建信号量
            dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
            for (int i = 0; i < count; i++) {
                NSInteger start = i * truck;
                NSInteger subDataLength = MIN(truck, data.length - start);
                NSData *subData = [data subdataWithRange:NSMakeRange(start, subDataLength)];
                NSMutableDictionary *params = [NSMutableDictionary dictionary];
                params[@"fileInfoType"] = fileInfoType;
                params[@"chunk"] = @(i);
                params[@"chunks"] = @(count);
                params[@"name"] = name;
                AFHTTPSessionManager *sessionManager = [AFHTTPSessionManager manager];
                NSURLSessionDataTask *task = [sessionManager POST:url parameters:params headers:nil constructingBodyWithBlock:^(id<AFMultipartFormData>  _Nonnull formData) {
                    [formData appendPartWithFileData:subData name:name fileName:name mimeType:@"video/mp4"];
                } progress:^(NSProgress * _Nonnull uploadProgress) {
                    // 上传进度，可以添加回调
                } success:^(NSURLSessionDataTask * _Nonnull task, id  _Nullable responseObject) {
                    [[self allSessionTask] removeObject:task];
                    if (i == count - 1) {
                        if ([responseObject[@"StatusData"][@"ResultData"] isKindOfClass:NSDictionary.class]) {
                            NSString *fileUrl = responseObject[@"StatusData"][@"ResultData"][@"FileUrl"];
                            // 确保在主线程调用resolve
                            dispatch_async(dispatch_get_main_queue(), ^{ 
                                resolve(fileUrl);
                            });
                        } else {
                            // 确保在主线程调用reject
                            dispatch_async(dispatch_get_main_queue(), ^{ 
                                reject(@"RESPONSE_ERROR", @"Invalid response format", nil);
                            });
                        }
                    }
                    dispatch_semaphore_signal(semaphore);
                } failure:^(NSURLSessionDataTask * _Nullable task, NSError * _Nonnull error) {
                    [[self allSessionTask] removeObject:task];
                    dispatch_semaphore_signal(semaphore);
                    NSLog(@"----上传失败-----%@", @(i));
                    NSLog(@"----失败原因-----%@", error);
                    // 确保在主线程调用reject
                    dispatch_async(dispatch_get_main_queue(), ^{ 
                        reject(@"UPLOAD_ERROR", error.localizedDescription, error);
                    });
                    return; // 失败了就不继续往下传下面的分片了
                }];
                [self.allSessionTask addObject:task];
                dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
            }
        });
    });
}

- (void)cancelUploadVideoTask {
    @synchronized(self) {
        [[self allSessionTask] enumerateObjectsUsingBlock:^(NSURLSessionDataTask * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            [obj cancel];
        }];
        [[self allSessionTask] removeAllObjects];
    }
}

- (NSString *)stringWithUUID {
    CFUUIDRef uuid = CFUUIDCreate(NULL);
    CFStringRef string = CFUUIDCreateString(NULL, uuid);
    CFRelease(uuid);
    return (__bridge_transfer NSString *)string;
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeEcoUploadVideoSpecJSI>(params);
}

@end
