package ru.sergeykozhukhov.backgroundworker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;

import java.lang.ref.WeakReference;

import ru.sergeykozhukhov.backgroundworker.View.FinanceProgressView;

public class MainActivity extends AppCompatActivity {

    /*
    * btn_start - кнопка для запуска работы
    * btn_pause - кнопка для паузы
    * btn_cancel - кнопка для отмены работы
    * fpv_progress_view - custom view, на примере котоого демонстрируется работы HandlerThread
    * */
    private Button btn_start;
    private Button btn_pause;
    private Button btn_cancel;
    private FinanceProgressView fpv_progress_view;

    /*
    * mWorker - фоновый поток
    * mState - состояния работы
    * mMainThreadHandler - handler для Looper UI потока
    * */
    private BackgroundWorker mWorker;
    private State mState;
    private Handler mMainThreadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews(); // инициализация визуальных компонентов
        setState(State.OFF); // установка начального состояния как "работа не ведется"
        initBackgroundWorker(); // инициализация потока для фоновой работы и handler для работы c Looper UI потока
        setOnClickListeners(); // инициализация обработчиков нажатия на кнопки
    }

    /*
    * Обработка ситуации, когда пользователь оставляет работу с данным activity
    * */
    @Override
    protected void onPause() {
        if (mState == State.RUNNING) {
            mWorker.pauseWork();
            setState(State.PAUSED);
        }
        super.onPause();
    }

    /*
    * Завершающий вызов перед тем, как activity будет уничтожена
    * */
    @Override
    protected void onDestroy() {
        if (isFinishing())  // проверка завершение работы с помощью метода finish() или завершения через систему
        {
            mWorker.quit();
            mWorker = null;
        }
        super.onDestroy();
    }

    /*
    * Инициализация потока для фоновой работы и handler для работы c Looper UI потока
    * */
    private void initBackgroundWorker() {
        mWorker = new BackgroundWorker("BackgroundWorker");
        mMainThreadHandler = new MainThreadHandler(this);
        mWorker.setClient(mMainThreadHandler);
        mWorker.start(); // запуск фонового потока
    }

    /*
    * Инициализация визуальных компонентов
    * */
    private void initViews() {
        btn_start = findViewById(R.id.buttonStart);
        btn_pause = findViewById(R.id.buttonPause);
        btn_cancel = findViewById(R.id.buttonCancel);
        fpv_progress_view = findViewById(R.id.progress);
        fpv_progress_view.setProgress(0);
    }

    /*
    * Инициализация обработчиков нажатия на кнопки
    * */
    private void setOnClickListeners() {
        // нажатие на кнопку "Старт"
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWorker.startWork(); // начало выполнения работы
                setState(State.RUNNING); // установка состояния - "работа запушена"
            }
        });

        // нажатие на кнопку "Пауза"
        btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final State newState;
                if (mState == State.PAUSED) {
                    mWorker.resumeWork(); // продолжение работы
                    newState = State.RUNNING; // состояние - "работа запушена"
                } else {
                    mWorker.pauseWork(); // пауза
                    newState = State.PAUSED; //состояние - "пауза"
                }
                setState(newState);
            }
        });

        // нажатие на кнопку "Отмена"
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWorker.cancelWork(); // отмена выполнения работы
                setState(State.OFF); // установка состояния - "работа прекращена/не ведется"
            }
        });
    }

    /*
    * Установка текущего состояния
    * Текущее состояниее определяет кликабельность определенных кнопок
    * */
    private void setState(@NonNull State state) {
        mState = state;
        switch (state) {
            case OFF: // в случае завершения работы
                btn_start.setEnabled(true); // открытие доступа к началу новой работы
                btn_pause.setEnabled(false); // отмена доступа к паузе
                btn_cancel.setEnabled(false); // отмена доступа к прекращению работы
                btn_pause.setText(getResources().getString(R.string.pause)); // текст кнопки "Пауза" - "пауза"
                break;
            case RUNNING: // в случае работы
                btn_start.setEnabled(false); // отмена доступа к началу новой работы
                btn_pause.setEnabled(true); // открытие доступа к паузе
                btn_cancel.setEnabled(true); // открытие доступа к прекращению работы
                btn_pause.setText(getResources().getString(R.string.pause)); // текст кнопки "Пауза" - "пауза"
                break;
            case PAUSED: // в случае паузы
                btn_start.setEnabled(false); // отмена доступа к началу новой работы
                btn_pause.setEnabled(true); // открытие доступа к паузе
                btn_cancel.setEnabled(true); // открытие доступа к прекращению работы
                btn_pause.setText(getResources().getString(R.string.resume)); // текст кнопки "Пауза" - "продолжить"
                break;
            default:
                throw new IllegalArgumentException("Unsupported state: " + state);

        }
    }

    /*
    * Допускаемые состояния работы
    * */
    private enum State {
        OFF, // работа прекращена/не ведется
        RUNNING, // работа запушена
        PAUSED // пауза
    }

    /*
    * Handler для работы с очередью сообщений главного потока.
    * */
    private static class MainThreadHandler extends Handler {
        /*
        * Слабая ссылка на данное activity
        * Ссылка слабая для того, чтобы в случае завершения activity при выполнении потока,
        * текущий класс не хранил ссылку на нее, и не происходило утечек памяти
        * */
        private final WeakReference<MainActivity> mActivityRef;

        private MainThreadHandler(@NonNull MainActivity activity) {
            super(Looper.getMainLooper()); // создание handler c Looper UI потока
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            MainActivity activity = mActivityRef.get();
            if (activity == null) {
                return; // обработка сообщений не производиться при null ссылки на activity
            }
            switch (msg.what) {
                case BackgroundWorker.MESSAGE_UPDATE_PROGRESS: // в случае ситуации с обновлением показателя прогресса
                    activity.fpv_progress_view.setProgress(msg.arg1); // устанавка значения данного показателя
                    break;
                case BackgroundWorker.MESSAGE_DONE: // в случае ситуации с завершением выполнения работы
                    activity.setState(State.OFF);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported message: " + msg.what);
            }
        }
    }


}
