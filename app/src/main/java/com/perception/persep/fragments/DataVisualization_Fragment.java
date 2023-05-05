package com.perception.persep.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.persep.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.perception.persep.data_gatherer.DataGathererManager;
import com.perception.persep.data_gatherer.GPSSensor;

import java.util.Comparator;
import java.util.LinkedList;

public class DataVisualization_Fragment extends Fragment implements GPSSensor.Listener
{
    private static final String TAG = "DataVisualizationFragment";


    LinkedList<DataPoint> m_DataPoints;
    GraphView m_GeolocationGraph;
    PointsGraphSeries<DataPoint> m_Series;

    DataGathererManager m_DataGathererManager;

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
        m_DataGathererManager.AddGPSListener(this);
        m_DataGathererManager.onStart();

        m_DataPoints = new LinkedList<>();
        m_Series = new PointsGraphSeries<>();
        m_Series.setSize(5.0f);

        m_GeolocationGraph = view.findViewById(R.id.geolocation_graph);

        m_GeolocationGraph.getViewport().setXAxisBoundsManual(true);
        m_GeolocationGraph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        m_GeolocationGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        m_GeolocationGraph.addSeries(m_Series);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        m_DataGathererManager.onStop();
    }

    private void AddData(DataPoint point)
    {
        m_DataPoints.addLast(point);
        m_DataPoints.sort(Comparator.comparing(DataPoint::getX));

        DataPoint[] dataPoints = m_DataPoints.toArray(new DataPoint[0]);
        m_Series.resetData(dataPoints);
        m_GeolocationGraph.getViewport().setMinX(m_Series.getLowestValueX());
        m_GeolocationGraph.getViewport().setMaxX(m_Series.getHighestValueX());
    }

    @Override
    public void onLocationChanged(double lat, double lon)
    {
        AddData(new DataPoint(lat, lon));
    }
}
