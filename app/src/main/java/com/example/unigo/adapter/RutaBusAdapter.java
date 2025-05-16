package com.example.unigo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unigo.R;
import com.example.unigo.model.RutaBus;

import java.util.List;

public class RutaBusAdapter extends RecyclerView.Adapter<RutaBusAdapter.ViewHolder> {

    private List<RutaBus> rutaList;
    private Context context;

    public RutaBusAdapter(Context context, List<RutaBus> rutaList) {
        this.context = context;
        this.rutaList = rutaList;
    }

    @NonNull
    @Override
    public RutaBusAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ruta_bus, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RutaBusAdapter.ViewHolder holder, int position) {
        RutaBus ruta = rutaList.get(position);

        holder.txtOrigen.setText("Origen: " + ruta.getOrigen());
        holder.txtDestino.setText("Destino: " + ruta.getDestino());
        holder.txtHora.setText("Salida: " + ruta.getDepartureTime());

        holder.txtRouteShortName.setText(ruta.getRouteShortName());
        holder.txtTripId.setText(ruta.getTripId());

        holder.imgRuta.setImageResource(R.drawable.icon_bus); // Asegúrate de tener este ícono en `drawable`
    }

    @Override
    public int getItemCount() {
        return rutaList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgRuta;
        TextView txtOrigen, txtDestino, txtHora, txtRouteShortName, txtTripId;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgRuta = itemView.findViewById(R.id.imgRuta);
            txtOrigen = itemView.findViewById(R.id.txtOrigen);
            txtDestino = itemView.findViewById(R.id.txtDestino);
            txtHora = itemView.findViewById(R.id.txtHora);
            txtRouteShortName = itemView.findViewById(R.id.txtRouteShortName);
            txtTripId = itemView.findViewById(R.id.txtTripId);
        }
    }
}
