# KOLO: Kotlin Only Looks Once

This repository contains a proof of concept application
for running the [Yolo](https://pjreddie.com/darknet/yolo/)
object detection model on a video stream writen in the Kotlin
programming language. The project uses an 
[experimental tensorflow based library](https://github.com/TomasVolker/komputo)
 for loading and executing the model.

To enable GPU, compile and add to the `lib` directory 
the Tensorflow for Java native library 
(`libtensorflow_jni.so` and `libtensorflow_jni.so`).
The frozen Tensorflow computation graphs have to be available in
 the `yolo` directory as `yolo.pb`and `tiny_yolo.pb`, 
 which can be obtained from 
[this](https://github.com/mystic123/tensorflow-yolo-v3) 
repository.

Once Tensorflow for Java and the frozen computation graphs
are set up, the application can be run using the gradle wrapper:

```
./gradlew run
```
And for tiny Yolo:
```
./gradlew run --args='--tiny'
```

By default, the first available camera device will be used 
(The webcam on a laptop for example). A url to a video
can be provided with the `--input` argument.