# 3dDCTVideoEncoding
Video encoding using three-dimensional DCT

This project contains an experimental video codec which uses much of the same techniques applied by the JPEG image encoder.
The key difference here is that the video frames are stacked so that a three dimensional DCT can be applied in blocks of 8x8x8 pixels
of the resulting frames stack.
There are multiple aspects of the codec which can be improved/researched:

- Only grayscale frames are supported so far, althought colored images can be enabled with some code changes.
- Using blocks of 8x8x8 effectively means that there is a key-frame every 8 frames. For comparison, a 24 fps video encoded with other
codecs like h264 usually have a max key frame interval of 5 times the framerate, which results in one key frame every 120 frames.
- JPEG uses pre-defined quantization matrices to encode and decode images. Instead of using pre-defined "quantization cubes", this project
is using a sub-optimal "quantization function" for encoding and decoding videos.
- A naive DCT algorithm is used, which is very slow to compute. There are two versions of the codec: the Java one does not
require any additional hardware to run, but takes a lot of time to encode/decode videos, althought it can use all available CPU cores. The
C implementation uses OpenCL to accelerate the DCT algorithm by orders of magnitude, but it requires a GPU to run.

To try the codec, the following steps must be executed:

1 - Create a raw source video with grayscale pixels (one byte per pixel) using the CaptureScreen utility of the Java project;
2 - Encode the captured video using the Encoder program of the Java project or the codec utility provided by the C project;
3 - Decode the resulting video using the Decoder program of the Java project or the codec utility provided by the C project;
4 - Watch the decoded video using the RenderVideo utility of the Java project.
