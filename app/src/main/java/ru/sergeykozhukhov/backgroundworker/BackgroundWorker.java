package ru.sergeykozhukhov.backgroundworker;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.Random;


/*
* Класс, обеспечивающий работу в фоне
* */
public class BackgroundWorker
        extends HandlerThread // HandlerThread - поток, имеющий Looper. Looper может быть использован для создания handler
{

    /*
    * MESSAGE_UPDATE_PROGRESS - значение аргемента сообщения для обновления показателя прогресса
    * MESSAGE_DONE - значение аргемента сообщения для завершения работы
    * */
    static final int MESSAGE_UPDATE_PROGRESS = -1;
    static final int MESSAGE_DONE = 0;

    /*
    * MESSAGE_START - значение аргемента сообщения для начала работы
    * MESSAGE_PAUSE - значение аргемента сообщения для паузы
    * MESSAGE_RESUME - значение аргемента сообщения для продолжения работы
    * MESSAGE_CANCEL - значение аргемента сообщения для отмены работы
    * MESSAGE_DO_NEXT_JOB - значение аргемента сообщения для выполнения следующего шага в работе
    * */
    private static final int MESSAGE_START = 1;
    private static final int MESSAGE_PAUSE = 2;
    private static final int MESSAGE_RESUME = 3;
    private static final int MESSAGE_CANCEL = 4;
    private static final int MESSAGE_DO_NEXT_JOB = 5;

    /*
    * JOB_MIN_TIME - минимальное время задержки работы потока
    * JOB_MAX_TIME - максимальное время задержки работы потока
    * */
    private static final int JOB_MIN_TIME = 30;
    private static final int JOB_MAX_TIME = 70;

    /*
    * mClient - handler для работы с другой очередью сообщений
    * mBackgroundHandler - handler для работы с сообщениями из текущего потока
    * */
    private Handler mClient;
    private Handler mBackgroundHandler;

    /*
    * mProgress - показатель прогресса
    * mRandom - случайное значение для задержки работы потока
    * */
    private int mProgress;
    private Random mRandom = new Random(System.currentTimeMillis());


    /*
    * Создание нового HandlerThread c именем потока name
    * */
    public BackgroundWorker(String name) {
        super(name); // под капотом имя name присваивается thread
    }

    /*
    * Подготовка данных перед запуском бесконечного цикла
    * */
    @Override
    protected void onLooperPrepared() {
        // инициализация mBackgroundHandler c Looper текущего потока
        // getLooper - получение ссылки на Looper текущего потока
        // Возможен вариант:
        // new Handler() - также инициализирует handler c Looper текущего потока
        mBackgroundHandler = new Handler(getLooper()) {
            // обработка приходящих сообшений в данный handler
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MESSAGE_START: // запуск работы
                        mProgress = 0; // обнуление показателя прогресса
                    case MESSAGE_DO_NEXT_JOB: // выполнение следующего шага
                    case MESSAGE_RESUME: // продолжение работы
                        sendProgress(mProgress); //передача сообщения с текущим показателем прогресса
                        doMoreWorkOrFinish();
                        break;
                    case MESSAGE_PAUSE: // пауза
                        // проверка
                        // имеется ли в очереди сообщение с аргументом what = MESSAGE_DO_NEXT_JOB
                        if (mBackgroundHandler.hasMessages(MESSAGE_DO_NEXT_JOB)) {
                            mProgress--; // вычитание единицы из прогресса
                        }
                        // удаление сообщений из очереди с what = MESSAGE_DO_NEXT_JOB
                        mBackgroundHandler.removeMessages(MESSAGE_DO_NEXT_JOB);
                        break;
                    case MESSAGE_CANCEL: // отмена работы
                        // удаление сообщений из очереди с what = параметрам
                        mBackgroundHandler.removeMessages(MESSAGE_START);
                        mBackgroundHandler.removeMessages(MESSAGE_PAUSE);
                        mBackgroundHandler.removeMessages(MESSAGE_RESUME);
                        mBackgroundHandler.removeMessages(MESSAGE_DO_NEXT_JOB);
                        sendProgress(0); // обнуление показатея прогресса
                        break;
                        default: // принимаем только обозначенные выше сообщения
                            throw new IllegalArgumentException("Unsupported message: " + msg.what);
                }
            }
        };
    }

    /*
     * Завершение работы Looper.
     * В очереди прекращается обработка сообщений.
     * */
    @Override
    public boolean quit() {
        mClient = null; // обнуление ссылки на mClient
        return super.quit();
    }

    /*
    * "Безопасное" завершение работы Looper.
    * В очереди обработаються только уже имеющиеся в ней сообщения.
    * Попытка добавить новые сообщения будет игнорироваться.
    * */
    @Override
    public boolean quitSafely() {
        mClient = null; // обнуление ссылки на mClient
        return super.quitSafely(); //
    }

    /*
    * Запуск работы
    * */
    void startWork(){
        sendBackgroundCommand(MESSAGE_START); // отправка сообщения с аргументом what на запуск
    }

    /*
    * Паузка
    * */
    void pauseWork(){
        sendBackgroundCommand(MESSAGE_PAUSE); // отправка сообщения с аргументом what на паузу
    }

    /*
    * Продолжение работы
    * */
    void resumeWork(){
        sendBackgroundCommand(MESSAGE_RESUME); // отправка сообщения с аргументом what на продолжение работы
    }

    /*
    * Отмена работы
    * */
    void cancelWork(){
        sendBackgroundCommand(MESSAGE_CANCEL); // отправка сообщения с аргументом what на отмену работы
    }

    /*
    * Установка mClient
    * @param client - handler
    * */
    void setClient(@NonNull Handler client) {
        mClient = client;
    }

    /*
    * Получение ссылки на mBackgroundHandler
    * */
    @NonNull
    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            throw new IllegalArgumentException("Handler is not ready yet");
        }
        return mBackgroundHandler;
    }

    /*
    * Передача сообщения с текущим показателем прогресса в очередь
    * */
    private void sendProgress(int progress){
        if (mClient != null){
            // создание сообщения с аргументами:
            // what = команде обновления прогресса,
            // arg1 = текущий прогресс,
            // arg2 = 0 (не значащий аргумент)
            // ОФИЦИАЛЬНЫЙ ИСТОЧНИК РЕКОМЕНДУЕТ ИСПОЛЬЗОВАТЬ obtainMessage вместо создания Message через его конструкторы,
            // т.к. это эффективнее и быстрее.
            // В этом случае сообщение достается из глобального пула сообщений, а не создается с нуля.
            Message updateProgress = mClient.obtainMessage(MESSAGE_UPDATE_PROGRESS, progress, 0);
            mClient.sendMessage(updateProgress); // передача сообщения
        }
    }


    /*
    * Выполнение работы до показателя прогресса равному 100
    * */
    private void doMoreWorkOrFinish() {
        if (mProgress >= 100) {
            Message message = mBackgroundHandler.obtainMessage(MESSAGE_DONE); // создание сообщения о завершении работы
            if (mClient != null) {
                mClient.sendMessage(message); // передача сообщения
            }
        } else {
            simulateHardWork(); // имитация бурной деятельности
            // создание сообщения
            // показатель прогресса увеличивается на единицу до присвоения аргументу
            Message message = mBackgroundHandler.obtainMessage(MESSAGE_DO_NEXT_JOB, ++mProgress, 0);
            mBackgroundHandler.sendMessage(message); // отправка сообщения в очередь
        }
    }

    /*
    * Имитация продолжительного исполнения работы
    * */
    private void simulateHardWork() {
        int jobTime; // случайное целое значение от JOB_MIN_TIME до JOB_MAX_TIME для имитации продолжительной работы
        jobTime = mRandom.nextInt(JOB_MAX_TIME - JOB_MIN_TIME) + JOB_MIN_TIME;
        try {
            // приостановление выполнения текущего потока на обозначенное время
            // выполняется приостановление исполнения потока, в котором данным статический метод был вызван
            Thread.sleep(jobTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
    * передача сообщения c атрибутом what = command
    * @param command - управляющая команда
    * */
    private void sendBackgroundCommand(int command) {
        Handler handler = getBackgroundHandler(); // получение ссылки на mBackgroundHandler
        Message message = handler.obtainMessage(command); // создание сообщения с what = command
        handler.sendMessage(message); // передача сообщения в очередь
    }


}
