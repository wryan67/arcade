package arcade;


import com.opencsv.CSVReader;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.ReflectionBuilder;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;


public class DisplayShelf extends Application {
    private static double WIDTH = 1000, HEIGHT = 900;
    private Stage primaryStage;
    public static final String BASEPATH="/games";

    private void initStage() {
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getBounds();

        WIDTH=primaryScreenBounds.getWidth();
        HEIGHT=primaryScreenBounds.getHeight();
        CloseAHK();
        ArcadeAHK();
        Group root = new Group();
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT));
//        primaryStage.fullScreenProperty();
        primaryStage.setFullScreen(true);
        // load images

//        System.out.println("Working Directory = " +
//                System.getProperty("user.dir"));

        CSVReader csvReader=null;
        try {
            csvReader = new CSVReader(new FileReader(BASEPATH+"/games.csv"), ',', '"', 0);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        ColumnPositionMappingStrategy strat = new ColumnPositionMappingStrategy();
        strat.setType(GameInfo.class);
        String[] columns = new String[] {"gameTypeText", "alias", "name", "imageFileName","ahkFileName"};   // the fields to bind do in your JavaBean
        strat.setColumnMapping(columns);

        CsvToBean csv = new CsvToBean();
        List<GameInfo> list = csv.parse(strat, csvReader);

        for (GameInfo gameInfo : list) {
            gameInfo.setType(GameType.typeOf(gameInfo.getGameTypeText().trim()));
            gameInfo.setName(gameInfo.getName().trim());
            gameInfo.setAlias(gameInfo.getAlias().trim());
            gameInfo.setImageFileName(gameInfo.getImageFileName().trim());
            System.out.println(gameInfo.getGameTypeText() + "|" + gameInfo.getName());
        }

        FxImage[] images = new FxImage[list.size()];
        for (int i = 0; i < list.size(); i++) {
            File imageFile=new File("/games/images/"+list.get(i).getImageFileName());
            try {
                images[i] = new FxImage(new FileInputStream(imageFile),list.get(i));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
        // create display shelf
        Shelf displayShelf = new Shelf(images,primaryStage);
        displayShelf.setPrefSize(WIDTH, HEIGHT);
        root.getChildren().add(displayShelf);
    }


    private static void CloseAHK() {
        if (!Config.joystick) return;

        List<String> cmd=new ArrayList<String>();
        cmd.add("c:\\Program Files\\AutoHotkey\\AutoHotkey.exe");
        cmd.add("c:\\games\\closeall.ahk");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File("c:\\games"));
        pb.redirectErrorStream(true);
        try {
            Process p=pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void startAHK(String ahkFileName) {
        if (!Config.joystick) return;
        System.out.println("starting autohotkey "+ahkFileName);
        List<String> cmd=new ArrayList<String>();
        cmd.add("c:\\Program Files\\AutoHotkey\\AutoHotkey.exe");
        cmd.add(ahkFileName);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File("c:\\games"));
        pb.redirectErrorStream(true);
        try {
            Process p=pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void ArcadeAHK() {
        startAHK("c:\\games\\arcade.ahk");
    }

    /**
     * A ui control which displays a browseble display shlef of images
     */
    public static class Shelf extends Region {
        private static final Duration DURATION = Duration.millis(500);
        private static final Interpolator INTERPOLATOR = Interpolator.EASE_BOTH;
        private static final double SPACING = 50;
        private static final double LEFT_OFFSET = -110;
        private static final double RIGHT_OFFSET = 110;
        private static final double SCALE_SMALL = 1.2;
        private final Stage primaryStage;
        private PerspectiveImage[] items;
        private Group centered = new Group();
        private Group left = new Group();
        private Group center = new Group();
        private Group right = new Group();
        private int centerIndex = 0;
        private Timeline timeline;
        private ScrollBar scrollBar = new ScrollBar();
        private boolean localChange = false;
        private Rectangle clip = new Rectangle();
        private GameInfo gameFocus;

        public Shelf(FxImage[] images, Stage primaryStage) {
            this.primaryStage=primaryStage;
            // set clip
            setClip(clip);
            // set background gradient using css
            setStyle("-fx-background-color: linear-gradient(to bottom," +
                    " black 60, #141414 60.1%, black 100%);");
            // style scroll bar color
            scrollBar.setStyle("-fx-base: #202020; -fx-background: #202020;");
            // create items
            items = new PerspectiveImage[images.length];
            for (int i = 0; i < images.length; i++) {
                final PerspectiveImage item =
                        items[i] = new PerspectiveImage(images[i]);

                items[i].setUserData(images[i].getInfo());
                final double index = i;

                item.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    public void handle(MouseEvent me) {
                        localChange = true;
                        scrollBar.setValue(index);
                        localChange = false;
                        shiftToCenter(item);
                    }
                });
            }
            // setup scroll bar
            scrollBar.setMax(items.length - 1);
            scrollBar.setVisibleAmount(1);
            scrollBar.setUnitIncrement(1);
            scrollBar.setBlockIncrement(1);
            scrollBar.valueProperty().addListener(new InvalidationListener() {
                public void invalidated(Observable ov) {
                    if (!localChange)
                        shiftToCenter(items[(int) scrollBar.getValue()]);
                }
            });

            // create content
            centered.getChildren().addAll(left, right, center);
            getChildren().addAll(centered, scrollBar);
            // listen for keyboard events
            setFocusTraversable(true);
            setOnKeyPressed(new EventHandler<KeyEvent>() {
                public void handle(KeyEvent ke) {
                    if (ke.getCode() == KeyCode.LEFT) {
                        shift(1);
                        localChange = true;
                        scrollBar.setValue(centerIndex);
                        localChange = false;
                    } else if (ke.getCode() == KeyCode.RIGHT) {
                        shift(-1);
                        localChange = true;
                        scrollBar.setValue(centerIndex);
                        localChange = false;
                    } else if (ke.getCode() == KeyCode.SPACE) {
                        rungame(gameFocus);
                    }
                }
            });
            // update
            update();
        }

        private void rungame(GameInfo gameInfo) {
            CloseAHK();
            if (!Util.isBlankOrNull(gameInfo.getAhkFileName())) {
                startAHK(gameInfo.getAhkFileName());
            }
            try {
                switch (gameInfo.getType()) {
                    case MAME  :  mame(gameInfo);      break;
                    case DOSBOX:  dosbox(gameInfo);    break;
                    case LAUNCH:  launch(gameInfo);    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            CloseAHK();
            ArcadeAHK();
        }



        private void mame(GameInfo gameInfo) throws InterruptedException, IOException {
            System.out.println("game started  at " + System.currentTimeMillis() / 1000);

            Path source=new File("c:/games/mame/cfg/default.master.cfg").toPath();
            Path target=new File("c:/games/mame/cfg/default.cfg").toPath();
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            List<String> cmd=new ArrayList<String>();

            cmd.add("c:\\games\\mame\\mame.exe");

            String args[]=gameInfo.getAlias().split("\\s+");
            cmd.add(args[0]);
            for (int i=1;i<args.length;++i) {
                boolean valid=true;

                if (!Config.allowRotate && args[i].toLowerCase().equals("-ror")) {
                    valid=false;
                }
                if (valid) {
                    cmd.add(args[i]);
                }
            }
            System.out.println("executing " + sprintList(cmd, " "));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File("c:\\games\\mame"));
            pb.redirectError(new File("NUL"));
            pb.redirectOutput(new File("NUL"));
            Process p=pb.start();
            System.out.println("game reading io at "+System.currentTimeMillis()/1000);
//            OutputStream os = p.getOutputStream();
//            os.write(' ');
//            os.flush();
//            os.close();
//            InputStream is = p.getInputStream();
//            try {
//                while (is.read()>=0) {}
//            } catch (Throwable t) {}
//            System.out.println("game io finished at "+System.currentTimeMillis()/1000);
            System.out.println("waiting for game to finish at "+System.currentTimeMillis()/1000);
            p.waitFor();
            System.out.println("mame finished at "+System.currentTimeMillis()/1000);
        }

        private String sprintList(List<String> cmd, String s) {
            StringBuilder tmpstr=new StringBuilder(1024);

            int i=0;
            for (i=0;i<cmd.size()-1;++i) {
                tmpstr.append(cmd.get(i));
                tmpstr.append(s);
            }
            if (cmd.size()>0) {
                tmpstr.append(i);
            }

            return tmpstr.toString();
        }

        private void add2cmd(List<String> cmd, StringBuilder cmdStr, String s) {
            cmd.add(s);
            cmdStr.append(s);
            cmdStr.append(" ");
        }


        private void dosbox(GameInfo gameInfo) throws InterruptedException, IOException {
            System.out.println("game started  at " + System.currentTimeMillis() / 1000);
            List<String> cmd=new ArrayList<String>();
            cmd.add("C:/Program Files (x86)/DOSBox-0.74/DOSBox.exe");
            String args[]=gameInfo.getAlias().split("\\s+");
            cmd.add("c:\\games\\dosbox\\" + args[0]);
            for (int i=1;i<args.length;++i) {
                cmd.add(args[i]);
            }

            cmd.add("-exit");
            cmd.add("-noconsole");

            for (int i=0;i<cmd.size();++i) {
                System.out.print(cmd.get(i)+" ");
            }
            System.out.println("");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File("c:\\games\\dosbox"));
            pb.redirectError(new File("NUL"));
            pb.redirectOutput(new File("NUL"));
            Process p=pb.start();
            System.out.println("game reading io at "+System.currentTimeMillis()/1000);
            System.out.println("waiting for doxbox to finish at "+System.currentTimeMillis()/1000);
            p.waitFor();
            System.out.println("dosbox finished at "+System.currentTimeMillis()/1000);
        }

        private void launch(GameInfo gameInfo) throws InterruptedException, IOException {
            System.out.println("game started  at " + System.currentTimeMillis() / 1000);
            primaryStage.toBack();
            List<String> cmd=new ArrayList<String>();
            String args[]=gameInfo.getAlias().split("\\s+");
            for (int i=0;i<args.length;++i) {
                cmd.add(args[i]);
            }

            for (int i=0;i<cmd.size();++i) {
                System.out.print(cmd.get(i)+" ");
            }
            System.out.println("");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File("c:\\games"));
            pb.redirectError(new File("NUL"));
            pb.redirectOutput(new File("NUL"));
            Process p=pb.start();
            System.out.println("game reading io at "+System.currentTimeMillis()/1000);
            System.out.println("waiting for doxbox to finish at "+System.currentTimeMillis()/1000);
            p.waitFor();
            System.out.println("launch finished at "+System.currentTimeMillis()/1000);
        }


        @Override
        protected void layoutChildren() {
            // update clip to our size
            clip.setWidth(getWidth());
            clip.setHeight(getHeight());
            // keep centered centered
            centered.setLayoutY((getHeight() - PerspectiveImage.HEIGHT) / 2);
            centered.setLayoutX((getWidth() - PerspectiveImage.WIDTH) / 2);

            // position scroll bar at bottom
            scrollBar.setLayoutX(10);
            scrollBar.setLayoutY(getHeight() - 25);
            scrollBar.resize(getWidth() - 20, 15);
        }

        private void update() {
            // move items to new homes in groups
            left.getChildren().clear();
            center.getChildren().clear();
            right.getChildren().clear();
            for (int i = 0; i < centerIndex; i++) {
                left.getChildren().add(items[i]);
            }
            center.getChildren().add(items[centerIndex]);
            for (int i = items.length - 1; i > centerIndex; i--) {
                right.getChildren().add(items[i]);
            }
            // stop old timeline if there is one running
            if (timeline != null) timeline.stop();
            // create timeline to animate to new positions
            timeline = new Timeline();
            // add keyframes for left items
            final ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
            for (int i = 0; i < left.getChildren().size(); i++) {
                final PerspectiveImage it = items[i];
                double newX = -left.getChildren().size() *
                        SPACING + SPACING * i + LEFT_OFFSET;
                keyFrames.add(new KeyFrame(DURATION,
                        new KeyValue(it.translateYProperty(), 0, INTERPOLATOR),
                        new KeyValue(it.translateXProperty(), newX, INTERPOLATOR),
                        new KeyValue(it.scaleXProperty(), SCALE_SMALL, INTERPOLATOR),
                        new KeyValue(it.scaleYProperty(), SCALE_SMALL, INTERPOLATOR),
                        new KeyValue(it.angle, 45.0, INTERPOLATOR)));
            }
            // add keyframe for center item
            final PerspectiveImage centerItem = items[centerIndex];
            gameFocus=(GameInfo)items[centerIndex].getUserData();
            keyFrames.add(new KeyFrame(DURATION,
                    new KeyValue(centerItem.translateYProperty(), 150, INTERPOLATOR),

                    new KeyValue(centerItem.translateXProperty(), 150, INTERPOLATOR),
                    new KeyValue(centerItem.scaleXProperty(), 2, INTERPOLATOR),
                    new KeyValue(centerItem.scaleYProperty(), 2, INTERPOLATOR),
                    new KeyValue(centerItem.angle, 90.0, INTERPOLATOR)));
            // add keyframes for right items
            for (int i = 0; i < right.getChildren().size(); i++) {
                final PerspectiveImage it = items[items.length - i - 1];
                final double newX = right.getChildren().size() *
                        SPACING - SPACING * i + RIGHT_OFFSET;
                keyFrames.add(new KeyFrame(DURATION,
                        new KeyValue(it.translateYProperty(), 0, INTERPOLATOR),
                        new KeyValue(it.translateXProperty(), newX, INTERPOLATOR),
                        new KeyValue(it.scaleXProperty(), SCALE_SMALL, INTERPOLATOR),
                        new KeyValue(it.scaleYProperty(), SCALE_SMALL, INTERPOLATOR),
                        new KeyValue(it.angle, 135.0, INTERPOLATOR)));
            }
            // play animation
            timeline.play();
        }

        private void shiftToCenter(PerspectiveImage item) {
            for (int i = 0; i < left.getChildren().size(); i++) {
                if (left.getChildren().get(i) == item) {
                    int shiftAmount = left.getChildren().size() - i;
                    shift(shiftAmount);
                    return;
                }
            }
            if (center.getChildren().get(0) == item) {
                GameInfo gameInfo=(GameInfo) item.getUserData();
                System.out.println("user clicked " + gameInfo.getName() );
                rungame(gameInfo);
                return;
            }
            for (int i = 0; i < right.getChildren().size(); i++) {
                if (right.getChildren().get(i) == item) {
                    int shiftAmount = -(right.getChildren().size() - i);
                    shift(shiftAmount);
                    return;
                }
            }
        }

        public void shift(int shiftAmount) {
            if (centerIndex <= 0 && shiftAmount > 0) return;
            if (centerIndex >= items.length - 1 && shiftAmount < 0) return;
            centerIndex -= shiftAmount;
            update();
        }
    }

    /**
     * A Node that displays a image with some 2.5D perspective rotation around the Y axis.
     */
    public static class PerspectiveImage extends Parent {
        private static final double REFLECTION_SIZE = 0.25;
        private static final double WIDTH = 200;
        private static final double HEIGHT = WIDTH + (WIDTH * REFLECTION_SIZE);
        private static final double RADIUS_H = WIDTH / 2;
        private static final double BACK = WIDTH / 10;
        private PerspectiveTransform transform = new PerspectiveTransform();
        /**
         * Angle Property
         */
        private final DoubleProperty angle = new SimpleDoubleProperty(45) {
            @Override
            protected void invalidated() {
                // when angle changes calculate new transform
                double lx = (RADIUS_H - Math.sin(Math.toRadians(angle.get())) * RADIUS_H - 1);
                double rx = (RADIUS_H + Math.sin(Math.toRadians(angle.get())) * RADIUS_H + 1);
                double uly = (-Math.cos(Math.toRadians(angle.get())) * BACK);
                double ury = -uly;
                transform.setUlx(lx);
                transform.setUly(uly);
                transform.setUrx(rx);
                transform.setUry(ury);
                transform.setLrx(rx);
                transform.setLry(HEIGHT + uly);
                transform.setLlx(lx);
                transform.setLly(HEIGHT + ury);
            }
        };

        public final double getAngle() {
            return angle.getValue();
        }

        public final void setAngle(double value) {
            angle.setValue(value);
        }

        public final DoubleProperty angleModel() {
            return angle;
        }

        public PerspectiveImage(Image image) {
            ImageView imageView = new ImageView(image);
            imageView.setEffect(ReflectionBuilder.create().fraction(REFLECTION_SIZE).build());
            setEffect(transform);
            getChildren().addAll(imageView);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage=primaryStage;
        initStage();
        primaryStage.show();
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX
     * application. main() serves only as fallback in case the
     * application can not be launched through deployment artifacts,
     * e.g., in IDEs with limited FX support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ArrayList<String> newArgs=new ArrayList<>();

        for(int i = 0; i < args.length; ++i) {
            if (args[i].toLowerCase().replaceAll("-","").equals("norotate")) {
                Config.allowRotate = false;
            } else if (args[i].toLowerCase().replaceAll("-","").equals("nojoystick")) {
                Config.joystick = false;
            } else {
                newArgs.add(args[i]);
            }
        }

        launch(newArgs.toArray(new String[newArgs.size()]));


    }

}
