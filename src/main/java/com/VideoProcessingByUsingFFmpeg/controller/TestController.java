package com.VideoProcessingByUsingFFmpeg.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.VideoProcessingByUsingFFmpeg.Service.VideoSrevice;

@RestController
@RequestMapping("vamsi")
public class TestController {
	@Autowired
	private VideoSrevice vs;
	@GetMapping("video")
	public String getOutputFile(@RequestParam("url") String url) throws IOException {
		return vs.videoProcessing(url);
		
	}
	

}
