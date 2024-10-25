package com.VideoProcessingByUsingFFmpeg.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class VideoSrevice {
	 private final static String ffmpegPath =
	 "C:\\Users\\admin\\Downloads\\ffmpeg-master-latest-win64-gpl-shared\\bin\\ffmpeg.exe";

	public String videoProcessing(String url) throws IOException {
		ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		File inputDirectory = new File(url);
		String outputDir = "C:\\Users\\admin\\Desktop\\processedvideolinks";

		// Ensure the output directory exists
		File outputDirectory = new File(outputDir);
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs(); // Create the directory if it does not exist
		}

		File[] listFiles = inputDirectory.listFiles();

		if (listFiles == null || listFiles.length == 0) {
			return "No video files found in the input directory.";
		}

		for (File file : listFiles) {

			if (file.isFile()) {
				System.out.println("Processing file: " + file.getName());

				// Loop through each resolution and process chunks for that resolution
				// for (ResolutionsForVideo rd : ResolutionsForVideo.values()) {

				VideoProcessingInMemory(file, outputDirectory, 256, 144);
				// processVideoSegments(file,256,144);
				// }
			}
		}

		// executorService.shutdown(); // Shutdown executor after processing
		return "Processing completed.";
	}
	// String chunkDirectoryPath = "C:\\Users\\admin\\Desktop\\chunks";

	public void videoProcessingInLocal(File inputFile, File outputDir, int width, int height) {
		// Calculate the segment size in bytes
		System.out.println("video segment");
		int segmentSizeBytes = 5 * 1024 * 1024;

		// Get the video bitrate to calculate the segment duration
		long bitrate = getVideoBitrate(inputFile);
		if (bitrate <= 0) {
			System.err.println("Could not determine the video bitrate. Please check the input file.");
			return; // Exit if bitrate could not be determined
		}

		// Calculate the segment duration in seconds based on the segment size and
		// bitrate
		int segmentDurationSeconds = (int) ((segmentSizeBytes * 8L) / bitrate); // Convert bytes to bits

		String outputFilePathTemplate = outputDir + File.separator + "vamsi%d.mp4";
		String pixelFormat = "yuv420p";

		// Construct the FFmpeg command to split the video into segments with resolution
		// change and pixel format
		String ffmpegCommand = String.format(
				"ffmpeg -i \"%s\" -vf \"scale=%d:%d\" -pix_fmt %s -c:v libx264 -preset fast -crf 23 -f segment -segment_time %d -reset_timestamps 1 \"%s\"",
				inputFile.getAbsolutePath(), width, height, pixelFormat, segmentDurationSeconds,
				outputFilePathTemplate);

		ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand.split(" "));

		try {
			// Start the process
			Process process = processBuilder.start();

			// Capture error stream output for debugging
			try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				String line;
				while ((line = errorReader.readLine()) != null) {
					System.err.println(line);
				}
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Video successfully split into segments.");
				combineSegments(outputDir);
			} else {
				System.err.println("Error splitting video into segments.");
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void combineSegments(File outputDir) {
		// Create a "concat" text file in the output directory
		File concatFile = new File(outputDir, "concat.txt");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(concatFile))) {
			// List all segment files in the output directory
			File[] segmentFiles = outputDir.listFiles((dir, name) -> name.matches("vamsi\\d+\\.mp4"));
			if (segmentFiles == null || segmentFiles.length == 0) {
				System.err.println("No video segments found to combine.");
				return;
			}

			// Sort segment files to ensure correct order
			Arrays.sort(segmentFiles, Comparator.comparing(File::getName));

			// Write the paths of segment files to the "concat" text file
			for (File segmentFile : segmentFiles) {
				writer.write("file '" + segmentFile.getAbsolutePath() + "'");
				writer.newLine();
			}

		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// Output file path for the combined video
		String outputFilePath = outputDir + File.separator + "combined_video.mp4";

		// FFmpeg command to combine the segments
		String ffmpegCommand = String.format("ffmpeg -f concat -safe 0 -i \"%s\" -c copy \"%s\"",
				concatFile.getAbsolutePath(), outputFilePath);

		ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand.split(" "));

		try {
			// Start the process
			Process process = processBuilder.start();

			// Capture error stream output for debugging
			try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				String line;
				while ((line = errorReader.readLine()) != null) {
					System.err.println(line);
				}
			}

			int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Segments successfully combined into a single video.");
			} else {
				System.err.println("Error combining video segments.");
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public long getVideoBitrate(File inputFile) {
		String ffmpegCommand = String.format("ffmpeg -i \"%s\"", inputFile.getAbsolutePath());
		ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand.split(" "));

		long bitrate = -1; // Default to -1 to indicate failure
		try {
			Process process = processBuilder.start();

			// Capture output
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("bitrate:")) {
					// Extract bitrate from the line
					String[] parts = line.split(","); // Split by comma
					for (String part : parts) {
						if (part.trim().startsWith("bitrate:")) {
							String bitrateStr = part.trim().split(" ")[1]; // Get the value
							// Convert to long
							bitrate = Long.parseLong(bitrateStr.replace("kbit/s", "").trim()) * 1000; // Convert to bits
																										// per second
							break;
						}
					}
				}
			}

			// Wait for the process to complete
			process.waitFor();
		} catch (IOException | InterruptedException | NumberFormatException e) {
			System.err.println("Failed to get bitrate: " + e.getMessage());
		}

		return bitrate; // Return the bitrate in bits per second
	}

	public void VideoProcessingInMemory(File inputFile, File outputDir, int width, int height) {
		System.out.println("Processing video segments based on size...");

        // Get the total duration of the video
        int totalDuration = getVideoDuration(inputFile.getAbsolutePath()); // Replace with your method
     int segmentduriation =5*1024*1024;
        // Create a ByteArrayOutputStream to hold the combined video segments in memory
        ByteArrayOutputStream combinedOutputStream = new ByteArrayOutputStream();

        // Loop to process each chunk
        int startTime = 0;

        while (startTime < totalDuration) {
            // Construct the FFmpeg command to process the segment
            String ffmpegCommand = String.format(
                "ffmpeg -i \"%s\" -vf \"scale=%d:%d\" -pix_fmt yuv420p -ss %d -t %d -f mpegts pipe:1",
                inputFile.getAbsolutePath(), width, height, startTime, segmentduriation
            );

            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand.split(" "));
            processBuilder.redirectErrorStream(true); // Combine stderr with stdout

            try {
                // Start the FFmpeg process
                Process process = processBuilder.start();
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder errorOutput = new StringBuilder();
                    String errorLine;

                    System.out.println("FFmpeg Error Output:");
                    while ((errorLine = errorReader.readLine()) != null) {
                        System.err.println(errorLine); // Print each error line immediately
                        errorOutput.append(errorLine).append("\n"); // Append to capture the entire error log
                    }

                    if (errorOutput.length() != 0) {
                        System.err.println("Detailed FFmpeg Error:\n" + errorOutput);
                    }
                }

                // Capture the pipe output using a BufferedInputStream
                try (BufferedInputStream inputStream = new BufferedInputStream(process.getInputStream())) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    // Write MPEG-TS data to the combined output stream
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        combinedOutputStream.write(buffer, 0, bytesRead);
                    }
                }

                // Check for errors in the FFmpeg process
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    System.err.println("FFmpeg process failed with exit code: " + exitCode);
                 
                    return; // Exit if FFmpeg fails
                }

                // Move to the next chunk
                startTime += segmentduriation;

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return; // Exit on exception
            }
        }

        // Write the combined output to a final video file
        try (FileOutputStream fileOutputStream = new FileOutputStream(new File(outputDir, "combined_output.mp4"))) {
            combinedOutputStream.writeTo(fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

            
	}

	private static int getVideoDuration(String inputFile) {
		String ffmpegCommand = String.format("%s -i \"%s\"", ffmpegPath, inputFile);
	    try {
	        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand.split(" "));
	        processBuilder.redirectErrorStream(true); // Combine stderr with stdout
	        Process process = processBuilder.start();

	        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
	            String line;
	            while ((line = reader.readLine()) != null) {
	                System.out.println("FFmpeg Output: " + line); // Log the entire line for debugging
	                if (line.contains("Duration:")) {
	                    // Example of expected line: "Duration: 00:02:12.40, start: 0.000000, bitrate: 2088 kb/s"
	                    String durationString = line.split(",")[0].split(": ")[1].trim(); // Extract the duration part

	                    // Split the duration string into components
	                    String[] parts = durationString.split(":");
	                    if (parts.length == 3) { // Ensure there are hours, minutes, and seconds
	                        int hours = Integer.parseInt(parts[0]);
	                        int minutes = Integer.parseInt(parts[1]);
	                        double secondsWithFraction = Double.parseDouble(parts[2]);
	                        int seconds = (int) secondsWithFraction; // Convert fractional seconds to int
	                        return hours * 3600 + minutes * 60 + seconds;
	                    } else {
	                        System.err.println("Unexpected duration format: " + durationString);
	                        return -1; // Invalid format
	                    }
	                }
	            }
	        }
	        process.waitFor();
	    } catch (IOException | InterruptedException | NumberFormatException e) {
	        e.printStackTrace();
	    }
	    return -1; // Return / 
	    }
}//ffmpeg -i "input.mp4" -vf "scale=1280:720" -pix_fmt yuv420p -c:v libx264 -crf 23 -b:v 2M -c:a aac -f mp4 output.mp4 -loglevel verbose
