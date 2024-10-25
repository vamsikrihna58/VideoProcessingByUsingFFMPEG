package com.VideoProcessingByUsingFFmpeg.Service;

public enum ResolutionsForVideo {
	
	ONE_FOURTY_FOUR_P(256,144),
	TWO_FOURTY_P(426,240),
	THREE_SIXTY_(640,360),
	FOUR_EIGHT_ZERO_P(640,480),
	SEVEN_TWO_ZERO_P(1280,720),
	ONE_ZERO_EIGHT_ZERO(1920,1080);
	  
	
	private int width;
	private int height;
	
	private ResolutionsForVideo(int w,int h) {
		this.width=w;
		this.height=h;
	}

	
	public int getHeight() {
		return height;
	}

	
	public int getWidth() {
		return width;
	}

	
	

}//144p: 256 x 144 pixels (16:9)
//240p: 426 x 240 pixels (16:9)
//360p: 640 x 360 pixels (16:9)
//480p (SD): 854 x 480 pixels (16:9)
//576p (SD): 1024 x 576 pixels (16:9)
//720p (HD): 1280 x 720 pixels (16:9)
//1080p (FHD): 1920 x 1080 pixels (16:9)
//1440p (2K): 2560 x 1440 pixels (16:9)
