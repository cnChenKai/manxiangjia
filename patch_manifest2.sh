sed -i '/<application/i \
    <provider\
        android:name="androidx.startup.InitializationProvider"\
        android:authorities="com.mangahaven.androidx-startup"\
        android:exported="false"\
        tools:node="merge">\
        <meta-data\
            android:name="androidx.work.WorkManagerInitializer"\
            tools:node="remove" \/>\
    <\/provider>\
' app/src/main/AndroidManifest.xml
