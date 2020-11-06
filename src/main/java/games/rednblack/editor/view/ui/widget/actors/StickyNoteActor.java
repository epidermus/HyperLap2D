package games.rednblack.editor.view.ui.widget.actors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.*;
import com.kotcrab.vis.ui.widget.color.ColorPicker;
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter;
import games.rednblack.editor.HyperLap2DFacade;
import games.rednblack.editor.controller.commands.ModifyStickyNoteCommand;
import games.rednblack.editor.renderer.data.StickyNoteVO;
import games.rednblack.editor.view.stage.Sandbox;
import games.rednblack.h2d.common.MsgAPI;
import games.rednblack.h2d.common.view.ui.StandardWidgetsFactory;
import games.rednblack.h2d.common.view.ui.widget.HyperLapColorPicker;

public class StickyNoteActor extends VisWindow {
    public String id;

    private final int MOVE = 1 << 5;

    private float worldX, worldY;
    private final Vector2 tmp = new Vector2();
    private final VisTextArea contentArea;
    private final HyperLap2DFacade facade = HyperLap2DFacade.getInstance();
    private final VisImageButton pinButton;

    private int resizeBorder = 8;

    public StickyNoteActor(String id) {
        super("", "sticky-note");
        pinButton = new VisImageButton("sticky-note-pin");
        pinButton.setX(-pinButton.getWidth() / 2f);
        pinButton.setY(-pinButton.getHeight() / 2f);
        this.getTitleTable().addActor(pinButton);

        setMoveListener();

        this.id = id;

        setKeepWithinParent(false);
        setKeepWithinStage(false);
        setResizable(true);

        contentArea = StandardWidgetsFactory.createTextArea("sticky-note");
        contentArea.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                StickyNoteVO payload = ModifyStickyNoteCommand.payload(StickyNoteActor.this);
                facade.sendNotification(MsgAPI.ACTION_MODIFY_STICKY_NOTE, payload);
            }
        });
        contentArea.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return button == Input.Buttons.RIGHT || super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.RIGHT) {
                    showPopupMenu();
                }
            }
        });
        add(contentArea).padTop(5).padLeft(5).grow();
        setOrigin(Align.topLeft);
    }

    @Override
    public void close() {
        addAction(Actions.sequence(Actions.scaleTo(0, 0, 0.3f, Interpolation.swingIn), new Action() {
            @Override
            public boolean act (float delta) {
                remove();
                return true;
            }
        }));
    }

    public void show(Group parent) {
        parent.addActor(this);
        Action action = Actions.parallel(Actions.parallel(Actions.alpha(0), Actions.alpha(1, 0.125f)),
                Actions.sequence(Actions.rotateBy(20),
                        Actions.rotateBy(-35, .25f, Interpolation.smoother),
                        Actions.rotateBy(15, .25f, Interpolation.swingOut)));
        addAction(action);
    }

    /**
     * Override stage position to world position
     * @param x position in World space
     * @param y position in World space
     */
    @Override
    public void setPosition(float x, float y) {
        worldX = x;
        worldY = y;
    }

    /**
     * Override stage position to world position
     * @param x position in World space
     */
    @Override
    public void setX(float x) {
        worldX = x;
    }

    /**
     * Override stage position to world position
     * @param y position in World space
     */
    @Override
    public void setY(float y) {
        worldY = y;
    }

    /**
     * Override stage position to world position
     * @param x position in World space
     * @param y position in World space
     * @param width width size
     * @param height height size
     */
    @Override
    public void setBounds(float x, float y, float width, float height) {
        tmp.set(x, y);
        Sandbox.getInstance().screenToWorld(tmp);
        worldX = tmp.x;
        worldY = tmp.y;

        setSize(width, height);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        tmp.set(worldX, worldY);
        Sandbox.getInstance().worldToScreen(tmp);
        if (getActions().size == 0) {
            float scale = Sandbox.getInstance().getZoomPercent() / 100f;
            setScale(scale > 1 ? 1f : scale);
        }
        super.setPosition(tmp.x, tmp.y);
        super.draw(batch, parentAlpha);
    }

    public String getContent() {
        return contentArea.getText();
    }

    public float getWorldX() {
        return worldX;
    }

    public float getWorldY() {
        return worldY;
    }

    public void setContent(String content) {
        contentArea.setText(content);
    }

    @Override
    public void setResizeBorder(int resizeBorder) {
        this.resizeBorder = resizeBorder;
        super.setResizeBorder(resizeBorder);
    }

    private void setMoveListener() {
        clearListeners();
        addListener(new InputListener() {
            float startX, startY, lastX, lastY;

            private void updateEdge (float x, float y) {
                float border = resizeBorder / 2f;
                float width = getWidth(), height = getHeight();
                float padTop = getPadTop(), padLeft = getPadLeft(), padBottom = getPadBottom(), padRight = getPadRight();
                float left = padLeft, right = width - padRight, bottom = padBottom;
                edge = 0;
                if (isResizable() && x >= left - border && x <= right + border && y >= bottom - border) {
                    if (x < left + border) edge |= Align.left;
                    if (x > right - border) edge |= Align.right;
                    if (y < bottom + border) edge |= Align.bottom;
                    if (edge != 0) border += 25;
                    if (x < left + border) edge |= Align.left;
                    if (x > right - border) edge |= Align.right;
                    if (y < bottom + border) edge |= Align.bottom;
                }
                if (isMovable() && edge == 0 && y <= height && y >= height - padTop && x >= left && x <= right) edge = MOVE;
            }

            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.LEFT) {
                    updateEdge(x, y);
                    dragging = edge != 0;
                    startX = x;
                    startY = y;
                    lastX = x - getWidth();
                    lastY = y - getHeight();
                }
                return edge != 0 || isModal();
            }

            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.LEFT) {
                    dragging = false;
                    StickyNoteVO payload = ModifyStickyNoteCommand.payload(StickyNoteActor.this);
                    facade.sendNotification(MsgAPI.ACTION_MODIFY_STICKY_NOTE, payload);
                } else if (button == Input.Buttons.RIGHT) {
                    showPopupMenu();
                }
            }

            public void touchDragged (InputEvent event, float x, float y, int pointer) {
                if (!dragging) return;
                float width = getWidth(), height = getHeight();
                float windowX = getX(), windowY = getY();

                float minWidth = getMinWidth();
                float minHeight = getMinHeight();

                if ((edge & MOVE) != 0) {
                    float amountX = x - startX, amountY = y - startY;
                    windowX += amountX;
                    windowY += amountY;
                }
                if ((edge & Align.left) != 0) {
                    float amountX = x - startX;
                    if (width - amountX < minWidth) amountX = -(minWidth - width);
                    width -= amountX;
                    windowX += amountX;
                }
                if ((edge & Align.bottom) != 0) {
                    float amountY = y - startY;
                    if (height - amountY < minHeight) amountY = -(minHeight - height);
                    height -= amountY;
                    windowY += amountY;
                }
                if ((edge & Align.right) != 0) {
                    float amountX = x - lastX - width;
                    if (width + amountX < minWidth) amountX = minWidth - width;
                    width += amountX;
                }
                if ((edge & Align.top) != 0) {
                    float amountY = y - lastY - height;
                    if (height + amountY < minHeight) amountY = minHeight - height;
                    height += amountY;
                }
                setBounds(Math.round(windowX), Math.round(windowY), Math.round(width), Math.round(height));
            }

            public boolean mouseMoved (InputEvent event, float x, float y) {
                updateEdge(x, y);
                return isModal();
            }

            public boolean scrolled (InputEvent event, float x, float y, int amount) {
                return isModal();
            }

            public boolean keyDown (InputEvent event, int keycode) {
                return isModal();
            }

            public boolean keyUp (InputEvent event, int keycode) {
                return isModal();
            }

            public boolean keyTyped (InputEvent event, char character) {
                return isModal();
            }
        });
    }

    private void showPopupMenu() {
        PopupMenu popupMenu = new PopupMenu();
        MenuItem rename = new MenuItem("Remove note");
        rename.addListener(
                new ClickListener(Input.Buttons.LEFT) {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        facade.sendNotification(MsgAPI.ACTION_REMOVE_STICKY_NOTE, id);
                    }
                });
        popupMenu.addItem(rename);
        MenuItem changeColor = new MenuItem("Change color");
        changeColor.addListener(
                new ClickListener(Input.Buttons.LEFT) {
                    boolean init = false;
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        ColorPicker picker = new HyperLapColorPicker(new ColorPickerAdapter() {
                            @Override
                            public void finished(Color newColor) {
                                setColor(newColor);
                                StickyNoteVO payload = ModifyStickyNoteCommand.payload(StickyNoteActor.this);
                                facade.sendNotification(MsgAPI.ACTION_MODIFY_STICKY_NOTE, payload);
                            }

                            @Override
                            public void changed(Color newColor) {
                                if (init)
                                    setColor(newColor);
                            }
                        });
                        init = true;
                        picker.setColor(getColor());
                        Sandbox.getInstance().getUIStage().addActor(picker.fadeIn());
                    }
                });
        popupMenu.addItem(changeColor);

        popupMenu.setPosition(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY() - popupMenu.getHeight());
        Sandbox.getInstance().getUIStage().addActor(popupMenu);
    }
}