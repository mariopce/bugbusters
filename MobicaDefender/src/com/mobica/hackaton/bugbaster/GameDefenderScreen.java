package com.mobica.hackaton.bugbaster;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.andengine.engine.Engine.EngineLock;
import org.andengine.engine.camera.ZoomCamera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.controller.MultiTouch;
import org.andengine.input.touch.detector.PinchZoomDetector;
import org.andengine.input.touch.detector.PinchZoomDetector.IPinchZoomDetectorListener;
import org.andengine.input.touch.detector.ScrollDetector;
import org.andengine.input.touch.detector.ScrollDetector.IScrollDetectorListener;
import org.andengine.input.touch.detector.SurfaceScrollDetector;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.bitmap.BitmapTexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.adt.io.in.IInputStreamOpener;
import org.andengine.util.debug.Debug;

import com.mobica.hackaton.bugbaster.adt.card.Card;
import com.mobica.hackaton.bugbaster.adt.monsters.Monsters;
import com.mobica.hackaton.bugbaster.monsters.Monster;

import android.graphics.Point;
import android.util.Log;
import android.widget.Toast;

/**
 *
 */
public class GameDefenderScreen extends SimpleBaseGameActivity implements
		IOnSceneTouchListener, IScrollDetectorListener,
		IPinchZoomDetectorListener {

	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;
	private static final String TAG = "GameDefenderScreen";

	private ZoomCamera mZoomCamera;
	private BitmapTextureAtlas mCardDeckTexture;

	private Scene mScene;

	private HashMap<Card, ITextureRegion> mMonsterTotextureRegionMap;
	private SurfaceScrollDetector mScrollDetector;
	private PinchZoomDetector mPinchZoomDetector;
	private float mPinchZoomStartedCameraZoomFactor;
	private ITexture mMobicaLogoTexture;

	private TextureRegion mMobicaTextureRegion;
	private ArrayList<Monster> mMonsters;

	@Override
	public EngineOptions onCreateEngineOptions() {
		this.mZoomCamera = new ZoomCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		final EngineOptions engineOptions = new EngineOptions(true,
				ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(
						CAMERA_WIDTH, CAMERA_HEIGHT), this.mZoomCamera);

		if (MultiTouch.isSupported(this)) {
			if (MultiTouch.isSupportedDistinct(this)) {
				Log.d(TAG,
						"MultiTouch detected --> Both controls will work properly!");
			} else {
				Log.d(TAG,
						"MultiTouch detected, but your device has problems distinguishing between fingers.\n\nControls are placed at different vertical locations.");
			}
		} else {
			Log.d(TAG,
					"Sorry your device does NOT support MultiTouch!\n\n(Falling back to SingleTouch.)\n\nControls are placed at different vertical locations.");
		}

		return engineOptions;
	}

	@Override
	public void onCreateResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

		this.mCardDeckTexture = new BitmapTextureAtlas(
				this.getTextureManager(), 1024, 512, TextureOptions.BILINEAR);
		BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				this.mCardDeckTexture, this, "carddeck_tiled.png", 0, 0);
		this.mCardDeckTexture.load();

		this.mMonsterTotextureRegionMap = new HashMap<Card, ITextureRegion>();

		/* Extract the TextureRegion of each card in the whole deck. */
		for (final Card card : Card.values()) {
			final ITextureRegion cardTextureRegion = TextureRegionFactory
					.extractFromTexture(this.mCardDeckTexture,
							card.getTexturePositionX(),
							card.getTexturePositionY(), Card.CARD_WIDTH,
							Card.CARD_HEIGHT);
			this.mMonsterTotextureRegionMap.put(card, cardTextureRegion);
		}

		try {
			createMobicaLogo();
			createMonster();
		} catch (IOException e) {
			Debug.e(e);
		}

	}

	private void createMobicaLogo() throws IOException {
		this.mMobicaLogoTexture = new BitmapTexture(this.getTextureManager(),
				new IInputStreamOpener() {
					@Override
					public InputStream open() throws IOException {
						return getAssets().open("gfx/mobica_logo.png");
					}
				});

		this.mMobicaLogoTexture.load();
		this.mMobicaTextureRegion = TextureRegionFactory
				.extractFromTexture(this.mMobicaLogoTexture);

	}

	private void createMonster() throws IOException {
		// "gfx/last-guardian-sprites.png"

		Random generator = new Random();

		this.mMonsters = new ArrayList<Monster>();
		for (int i = 0; i < 5; i++) {
			Monster m = new Monster("gfx/last-guardian-sprites.png", this,
					new Point(generator.nextInt(800), generator.nextInt(400)));
			this.mMonsters.add(m);
			m.loadTexures();
		}

	}

	@Override
	public Scene onCreateScene() {
		mEngine.registerUpdateHandler(new FPSLogger());

		mScene = new Scene();
		mScene.setOnAreaTouchTraversalFrontToBack();

		/*
		 * Calculate the coordinates for the face, so its centered on the
		 * camera.
		 */
		final float centerX = (CAMERA_WIDTH - this.mMobicaTextureRegion
				.getWidth()) / 2;
		final float centerY = (CAMERA_HEIGHT - this.mMobicaTextureRegion
				.getHeight()) / 2;
		final Sprite mobicaLogo = new Sprite(centerX, centerY,
				this.mMobicaTextureRegion, this.getVertexBufferObjectManager());

		final PhysicsHandler physicsHandler = new PhysicsHandler(mobicaLogo);
		mobicaLogo.registerUpdateHandler(physicsHandler);

		mScene.attachChild(mobicaLogo);

		for (Monster m : this.mMonsters) {
			mScene.attachChild(m.createMsprite());
		}

		mScene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));

		mScrollDetector = new SurfaceScrollDetector(this);
		mPinchZoomDetector = new PinchZoomDetector(this);

		mScene.setOnSceneTouchListener(this);
		mScene.setTouchAreaBindingOnActionDownEnabled(true);

		/* The actual collision-checking. */
		mScene.registerUpdateHandler(new ColisionDetect(mobicaLogo, null));

		return this.mScene;
	}

	class ColisionDetect implements IUpdateHandler {

		private Sprite mMobicaLogo;

		public ColisionDetect(Sprite mobicaLogo, List<Monsters> monsters) {
			this.mMobicaLogo = mobicaLogo;
		}

		@Override
		public void reset() {
		}

		@Override
		public void onUpdate(final float pSecondsElapsed) {
			if (mMobicaLogo.collidesWith(mMobicaLogo)) {
				mMobicaLogo.setColor(1, 0, 0);
			} else {
				mMobicaLogo.setColor(0, 1, 0);
			}

			if (!mZoomCamera.isRectangularShapeVisible(mMobicaLogo)) {
				mMobicaLogo.setColor(1, 0, 1);
			}
		}
	}

	@Override
	public void onScrollStarted(final ScrollDetector pScollDetector,
			final int pPointerID, final float pDistanceX, final float pDistanceY) {
		final float zoomFactor = this.mZoomCamera.getZoomFactor();
		this.mZoomCamera.offsetCenter(-pDistanceX / zoomFactor, -pDistanceY
				/ zoomFactor);
	}

	@Override
	public void onScroll(final ScrollDetector pScollDetector,
			final int pPointerID, final float pDistanceX, final float pDistanceY) {
		final float zoomFactor = this.mZoomCamera.getZoomFactor();
		this.mZoomCamera.offsetCenter(-pDistanceX / zoomFactor, -pDistanceY
				/ zoomFactor);
	}

	@Override
	public void onScrollFinished(final ScrollDetector pScollDetector,
			final int pPointerID, final float pDistanceX, final float pDistanceY) {
		final float zoomFactor = this.mZoomCamera.getZoomFactor();
		this.mZoomCamera.offsetCenter(-pDistanceX / zoomFactor, -pDistanceY
				/ zoomFactor);
	}

	@Override
	public void onPinchZoomStarted(final PinchZoomDetector pPinchZoomDetector,
			final TouchEvent pTouchEvent) {
		this.mPinchZoomStartedCameraZoomFactor = this.mZoomCamera
				.getZoomFactor();
	}

	@Override
	public void onPinchZoom(final PinchZoomDetector pPinchZoomDetector,
			final TouchEvent pTouchEvent, final float pZoomFactor) {
		this.mZoomCamera.setZoomFactor(this.mPinchZoomStartedCameraZoomFactor
				* pZoomFactor);
	}

	@Override
	public void onPinchZoomFinished(final PinchZoomDetector pPinchZoomDetector,
			final TouchEvent pTouchEvent, final float pZoomFactor) {
		this.mZoomCamera.setZoomFactor(this.mPinchZoomStartedCameraZoomFactor
				* pZoomFactor);
	}

	@Override
	public boolean onSceneTouchEvent(final Scene pScene,
			final TouchEvent pSceneTouchEvent) {
		this.mPinchZoomDetector.onTouchEvent(pSceneTouchEvent);

		if (this.mPinchZoomDetector.isZooming()) {
			this.mScrollDetector.setEnabled(false);
		} else {
			if (pSceneTouchEvent.isActionDown()) {
				this.mScrollDetector.setEnabled(true);
			}
			this.mScrollDetector.onTouchEvent(pSceneTouchEvent);
		}

		return true;
	}

	private void addMonster(final Card pCard, final int pX, final int pY) {
		final Sprite sprite = new Sprite(pX, pY,
				this.mMonsterTotextureRegionMap.get(pCard),
				this.getVertexBufferObjectManager()) {

			@Override
			public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
					final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
				switch (pSceneTouchEvent.getAction()) {
				case TouchEvent.ACTION_DOWN:
					this.setScale(1.25f);
					this.detachSelf();
					break;
				}
				return true;
			}
		};

		this.mScene.attachChild(sprite);
		this.mScene.registerTouchArea(sprite);
	}

}
