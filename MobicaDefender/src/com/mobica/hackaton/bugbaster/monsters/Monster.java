package com.mobica.hackaton.bugbaster.monsters;

import java.io.IOException;
import java.io.InputStream;

import org.andengine.entity.sprite.Sprite;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.bitmap.BitmapTexture;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.util.adt.io.in.IInputStreamOpener;

import android.graphics.Point;

import com.mobica.hackaton.bugbaster.GameDefenderScreen;

public class Monster {
	private Sprite mSprite;
	private ITexture mMonsterTexture;
	private TextureRegion mMonsterTextureRegion;
	private String mPath;
	private GameDefenderScreen mContext;
	private Point mPosition;

	public Monster(final String path, final GameDefenderScreen context,
			Point position) throws IOException {

		this.mPath = path;
		this.mContext = context;
		this.mPosition = position;

	}

	public void loadTexures() throws IOException {
		this.mMonsterTexture = new BitmapTexture(mContext.getTextureManager(),
				new IInputStreamOpener() {
					@Override
					public InputStream open() throws IOException {
						return mContext.getAssets().open(mPath);
					}
				});

		this.mMonsterTexture.load();
		this.mMonsterTextureRegion = TextureRegionFactory
				.extractFromTexture(this.mMonsterTexture);
	}

	public Sprite createMsprite() {
		final Sprite monster = new Sprite(mPosition.x, mPosition.y,
				this.mMonsterTextureRegion,
				mContext.getVertexBufferObjectManager());
		return monster;
	}

	/**
	 * @return the mSprite
	 */
	public Sprite getmSprite() {
		return mSprite;
	}

}
