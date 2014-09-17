package com.itgrupo.selfiefan;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffXfermode;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private LayoutInflater myInflater = null;
	private Camera myCamera;
	byte[] tempdata;
	boolean myPreviewRunning = false;
	private SurfaceHolder mySurfaceHolder = null;
	private SurfaceView mySurfaceView;
	SurfaceHolder.Callback sh_ob = null;
	SurfaceHolder.Callback sh_callback = null;
	Button takePicture;
	private boolean yn_Camara_Frontal = true;
	private boolean yn_Primera_Vez = true;
	private int num_Camaras = 0;
	Button Cambiar_Camara;
	Button takeFourPictures;
	Timer mTimer;
	MyTimerTask mTimerTask;
	private int intervalo_foto = 5000;
	private int num_fotos = 4;
	private boolean yn_Cuatro = false;
	Bitmap bm1;
	Bitmap bm2;
	Bitmap bm3;
	Bitmap bm4;
	private int parte_foto = 1;
	private static final String YN_CAMARA_FRONTAL_KEY = "camara";
	private static final String YN_PRIMERA_VEZ_KEY = "primera";
	private static final String NUM_CAMARAS_KEY = "num_camaras";
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null) {
			yn_Camara_Frontal = savedInstanceState.getBoolean(YN_CAMARA_FRONTAL_KEY);
			yn_Primera_Vez = savedInstanceState.getBoolean(YN_PRIMERA_VEZ_KEY);
			num_Camaras = savedInstanceState.getInt(NUM_CAMARAS_KEY);
		}

		if (yn_Primera_Vez) {
			//Obtengo número de camaras
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			num_Camaras = Camera.getNumberOfCameras();
			if (num_Camaras == 1) {
				Camera.getCameraInfo(0, cameraInfo);
				if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
					yn_Camara_Frontal = false;
				} else {
					if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
						yn_Camara_Frontal = true;
					}	
				}				
			}
			yn_Primera_Vez = !yn_Primera_Vez;
		}
		
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_main);
		
		mySurfaceView = (SurfaceView) findViewById(R.id.surface);
		if (mySurfaceHolder == null) {
			mySurfaceHolder = mySurfaceView.getHolder();
		}
		
		sh_callback = my_callback();
		mySurfaceHolder.addCallback(sh_callback);
		//mySurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		myInflater = LayoutInflater.from(this);
		View overView = myInflater.inflate(R.layout.segundacapa, null);
		this.addContentView(overView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		takePicture = (Button) findViewById(R.id.btn_TomarFoto);
		takePicture.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				myCamera.takePicture(myShutterCallback, myPictureCallback, myJpeg);
			}
		});

		Cambiar_Camara = (Button) findViewById(R.id.btn_Cambiar);
		
		if (num_Camaras == 1) {
			Cambiar_Camara.setVisibility(View.GONE);
		} else {
			Cambiar_Camara.setVisibility(View.VISIBLE);
		}
		
		Cambiar_Camara.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				yn_Camara_Frontal = !yn_Camara_Frontal;
				DefinirCamara(mySurfaceHolder);
				CambiarCamara(mySurfaceHolder);
			}
		});

		takeFourPictures = (Button) findViewById(R.id.btn_CuatroFotos);
		takeFourPictures.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mTimer != null) {
					mTimer.cancel();
				}
				mTimer = new Timer();
				
				mTimerTask = new MyTimerTask();
				yn_Cuatro = true;
				mTimer.schedule(mTimerTask, 2000, intervalo_foto);
			}
		});
	}
	
	ShutterCallback myShutterCallback = new ShutterCallback() {
		@Override
		public void onShutter() {
		}
	};
	
	PictureCallback myPictureCallback = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera myCamera) {
		}
	};
	
	PictureCallback myJpeg = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera myCamera) {
			if (data != null) {
				tempdata = data;
				done();
			}
		}
	};
	
	void done() {
		if (!yn_Cuatro) {
			Bitmap bm = BitmapFactory.decodeByteArray(tempdata, 0, tempdata.length);
			//String url = Images.Media.insertImage(getContentResolver(), bm, null, null);
			String url = guardarImagen(getApplicationContext(),"SelfieFan_", bm);
			//ImageView iv = (ImageView)findViewById(R.id.imgView);
			//iv.setImageBitmap(BitmapFactory.decodeFile(url));
			Guardar_Galeria(url);

			bm.recycle();
			Bundle bundle = new Bundle();
			if (url != null) {
				bundle.putString("url", url);
				Intent mIntent = new Intent();
				mIntent.putExtras(bundle);
				setResult(RESULT_OK, mIntent);
				Toast.makeText(this, url, Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(this, "Picture can not be saved", Toast.LENGTH_SHORT).show();
			}
			myCamera.startPreview();			
		} else {
			switch (parte_foto) {
			case 1:
				bm1 = BitmapFactory.decodeByteArray(tempdata, 0, tempdata.length);
				break;
			case 2:
				bm2 = BitmapFactory.decodeByteArray(tempdata, 0, tempdata.length);
				break;
			case 3:
				bm3 = BitmapFactory.decodeByteArray(tempdata, 0, tempdata.length);
				break;
			case 4:
				bm4 = BitmapFactory.decodeByteArray(tempdata, 0, tempdata.length);
				Procesar_Parte(bm4);
				break;
			default:
				break;
			}
			parte_foto += 1;
		}
	}
	
	void Procesar_Parte(Bitmap bm) {
		//Graphics g;
		
		if (parte_foto == 4) {
			Bitmap drawingBitmap = Bitmap.createBitmap(bm1.getWidth(),bm1.getHeight (), bm1.getConfig());
			Canvas canvas1 = new Canvas(drawingBitmap);
			Paint paint1 = new Paint();
			canvas1.drawBitmap(bm1, 0, 0, paint1);
			paint1.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.MULTIPLY));
			canvas1.drawBitmap(bm2, 0, 0, paint1);
			//compositeImageView.setImageBitmap(drawingBitmap);
		} else {
			Bundle bundle = new Bundle();
			if (bm != null) {
				bundle.putString("url", "OK " + parte_foto);
				Intent mIntent = new Intent();
				mIntent.putExtras(bundle);
				setResult(RESULT_OK, mIntent);
				Toast.makeText(this, "url", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(this, "Picture can not be saved", Toast.LENGTH_SHORT).show();
			}
		}
		myCamera.startPreview();
	}
	
	void Guardar_Galeria(final String url) {
		new MediaScannerConnectionClient() {
			private MediaScannerConnection msc = null; {
				msc = new MediaScannerConnection(getApplicationContext(), this); msc.connect();
			}
			public void onMediaScannerConnected() { 
				msc.scanFile(url, null);
			}
			public void onScanCompleted(String path, Uri uri) { 
				msc.disconnect();
			} 
		};
	}
	
	private String guardarImagen (Context context, String nombre, Bitmap imagen){
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
	    ContextWrapper cw = new ContextWrapper(context);
	    File dirImages = cw.getDir("Imagenes", Context.MODE_PRIVATE);
	    File myPath = new File(dirImages, nombre + timeStamp + ".jpg");
	    
	    FileOutputStream fos = null;
	    try{
	        fos = new FileOutputStream(myPath);
	        imagen.compress(Bitmap.CompressFormat.JPEG, 80, fos);
	        fos.flush();
	        fos.close();
	    }catch (FileNotFoundException ex){
	        ex.printStackTrace();
	    }catch (IOException ex){
	        ex.printStackTrace();
	    }
	    return myPath.getAbsolutePath();
	}
	
	private void DefinirCamara(SurfaceHolder holder) {
		if (myPreviewRunning) {
			myCamera.stopPreview();
			myPreviewRunning = false;
			myCamera.release();
			myCamera = null;
		}
		if (yn_Camara_Frontal) {
			myCamera = Camera.open(CameraInfo.CAMERA_FACING_FRONT);
		} else {
			myCamera = Camera.open(CameraInfo.CAMERA_FACING_BACK);
		}
		try {
			myCamera.setPreviewDisplay(holder);
		} catch (IOException exception){
			myCamera.release();
			myCamera = null;
		}
	}
	
	private void CambiarCamara(SurfaceHolder holder) {
		if (myPreviewRunning) {
			myCamera.stopPreview();
			myPreviewRunning = false;
		}
		Camera.Parameters p = myCamera.getParameters();
		//p.setPreviewSize(width, height);
		if(getResources().getDisplayMetrics().widthPixels < getResources().getDisplayMetrics().heightPixels) {
			myCamera.setDisplayOrientation(90);
		}
		myCamera.setParameters(p);
		try {
			myCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		myCamera.startPreview();
		myCamera.autoFocus(null);
		myPreviewRunning =true;			
	}
	
	SurfaceHolder.Callback my_callback() {
		SurfaceHolder.Callback ob1 = new SurfaceHolder.Callback() {
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				myCamera.stopPreview();
				myPreviewRunning = false;
				myCamera.release();
				myCamera = null;
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				DefinirCamara(holder);
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				CambiarCamara(holder);
			}
		};
		return ob1;
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putBoolean(YN_CAMARA_FRONTAL_KEY, yn_Camara_Frontal);
		savedInstanceState.putBoolean(YN_PRIMERA_VEZ_KEY, yn_Primera_Vez);
		savedInstanceState.putInt(NUM_CAMARAS_KEY, num_Camaras);
		super.onSaveInstanceState(savedInstanceState);
	}
	
	class MyTimerTask extends TimerTask {
		@Override
		public void run() {
			/*Calendar calendar = Calendar.getInstance();
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd:MMMM:yyyy HH:mm:ss a");
			final String strDate = simpleDateFormat.format(calendar.getTime());*/
			   
			runOnUiThread(new Runnable(){
				@Override
				public void run() {
					//takeFourPictures.setText(strDate);
					myCamera.takePicture(myShutterCallback, myPictureCallback, myJpeg);
					num_fotos -= 1;
					if (num_fotos <= 0) {
						mTimer.cancel();
						num_fotos = 4;
					}
			}});
		}
	}
}