package com.perception.persep.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.persep.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.perception.persep.data_gatherer.AccelerometerSensor;
import com.perception.persep.data_gatherer.DataGathererManager;


import java.util.LinkedList;

public class DataVisualization_Fragment extends Fragment implements AccelerometerSensor.Listener
{
    private static final String TAG = "DataVisualizationFragment";

    LinkedList<DataPoint> m_AccX;
    LineGraphSeries<DataPoint> m_AccXSeries;

    LinkedList<DataPoint> m_AccZ;
    LineGraphSeries<DataPoint> m_AccZSeries;

    int index = 0;
    int MAX_DATA_SIZE = 10;

    GraphView m_AccelerationGraph;

    DataGathererManager m_DataGathererManager;

    public DataVisualization_Fragment()
    {
        m_AccX = new LinkedList<>();
        for(int i=0;i<MAX_DATA_SIZE;i++) m_AccX.add(new DataPoint(i, 0));
        m_AccXSeries = new LineGraphSeries<>();
        m_AccXSeries.setColor(Color.RED);

        m_AccZ = new LinkedList<>();
        for(int i=0;i<MAX_DATA_SIZE;i++) m_AccZ.add(new DataPoint(i, 0));
        m_AccZSeries = new LineGraphSeries<>();
        m_AccZSeries.setColor(Color.BLUE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.datavisulization_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        m_DataGathererManager = new DataGathererManager(getActivity());
        m_DataGathererManager.AddAccelerometerListener(this);
        m_DataGathererManager.onStart();
        m_DataGathererManager.onResume();

        m_AccelerationGraph = view.findViewById(R.id.acceleration_graph);
        m_AccelerationGraph.getViewport().setYAxisBoundsManual(true);
        m_AccelerationGraph.getViewport().setXAxisBoundsManual(true);
        m_AccelerationGraph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        m_AccelerationGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        m_AccelerationGraph.addSeries(m_AccXSeries);
        m_AccelerationGraph.addSeries(m_AccZSeries);

        m_AccelerationGraph.getViewport().setMinX(0);
        m_AccelerationGraph.getViewport().setMaxX(MAX_DATA_SIZE);
        m_AccelerationGraph.getViewport().setMinY(-5);
        m_AccelerationGraph.getViewport().setMaxY(5);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        m_DataGathererManager.onStop();
        m_DataGathererManager.onPause();
    }

    @Override
    public void onAccelerationChanged(float[] acceleration)
    {
        m_AccX.set(index, new DataPoint(index, acceleration[0]));
        m_AccZ.set(index, new DataPoint(index, acceleration[2]));
        index = (index + 1) % MAX_DATA_SIZE;

        m_AccXSeries.resetData(m_AccX.toArray(new DataPoint[0]));
        m_AccZSeries.resetData(m_AccZ.toArray(new DataPoint[0]));
    }
}
