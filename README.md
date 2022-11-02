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

To build the C code, you will need the GNU C Compiler and Make utility. Open the file Default/makefile and change the variables RM and EXECUTABLENAME to suit your operating system and toolchain. The variable OPENCL_LIBRARY should also be changed to point to the location of the OpenCL dynamic load library (on Windows it is usually located at C:\Windows\System32\OpenCL.dll). It is also necessary to obtain the OpenCL headers, which can be cloned from https://github.com/KhronosGroup/OpenCL-Headers. The path to these headers must be set in the OPENCL_HEADERS_PATH variable inside the makefile.

You can also use the OpenCL version of the codec with integrated Intel GPUs that support the OpenCL technology, but you will probably need to lower the DCT_BLOCK_WIDTH, DCT_BLOCK_HEIGHT or DCT_BLOCK_DEPTH values declared at files codec.h and 3dDCT.cl (changing any of them to 4 is a good option).

To try the codec, the following steps must be executed:

- Create a raw source video with grayscale pixels (one byte per pixel) using the CaptureScreen utility of the Java project;
- Encode the captured video using the Encoder program of the Java project (slooooow) or the codec utility provided by the C project;
- Decode the resulting video using the Decoder program of the Java project (also slow) or the codec utility provided by the C project;
- Watch the decoded video using the RenderVideo utility of the Java project.
