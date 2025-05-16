package com.example.unigo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unigo.R;
import com.example.unigo.model.RutaBusTransb;

import java.util.List;

public class RutaTransbordoAdapter extends RecyclerView.Adapter<RutaTransbordoAdapter.RutaTransbordoViewHolder> {
    private List<RutaBusTransb> rutasTransbordo;

    public RutaTransbordoAdapter(List<RutaBusTransb> rutasTransbordo) {
        this.rutasTransbordo = rutasTransbordo;
    }

    @NonNull
    @Override
    public RutaTransbordoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ruta_transbordo, parent, false);
        return new RutaTransbordoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RutaTransbordoViewHolder holder, int position) {
        RutaBusTransb ruta = rutasTransbordo.get(position);

        holder.txtRuta1.setText("Línea " + ruta.getRuta1());
        holder.txtOrigen.setText(ruta.getOrigen());
        holder.txtSalidaOrigen.setText(ruta.getSalidaOrigen());
        holder.txtTransbordo.setText("Transbordo en: " + ruta.getTransbordo());
        holder.txtRuta2.setText("Línea " + ruta.getRuta2());
        holder.txtDestino.setText(ruta.getDestino());
        holder.txtSalidaTransbordo.setText(ruta.getSalidaTransbordo());
    }

    @Override
    public int getItemCount() {
        return rutasTransbordo.size();
    }

    public static class RutaTransbordoViewHolder extends RecyclerView.ViewHolder {
        TextView txtRuta1, txtOrigen, txtSalidaOrigen, txtTransbordo, txtRuta2, txtDestino, txtSalidaTransbordo;

        public RutaTransbordoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRuta1 = itemView.findViewById(R.id.txtRuta1);
            txtOrigen = itemView.findViewById(R.id.txtOrigen);
            txtSalidaOrigen = itemView.findViewById(R.id.txtSalidaOrigen);
            txtTransbordo = itemView.findViewById(R.id.txtTransbordo);
            txtRuta2 = itemView.findViewById(R.id.txtRuta2);
            txtDestino = itemView.findViewById(R.id.txtDestino);
            txtSalidaTransbordo = itemView.findViewById(R.id.txtSalidaTransbordo);
        }
    }
}
